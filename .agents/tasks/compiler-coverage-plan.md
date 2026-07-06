# Capturing test coverage of Spine Compiler–executed code

**Problem overview & implementation plan**

> Target repo: **`SpineEventEngine/compiler`** (the `io.spine.compiler` Gradle plugin /
> `compiler-gradle-plugin`, run via `compiler-fat-cli`).
> Written from an investigation in `SpineEventEngine/validation`. Self-contained: everything a
> fresh session needs is below, but every claim about the compiler's *internals* is stated as an
> assumption to **verify in this repo** (it was inferred from the consumer side).

---

## 1. TL;DR

Code that runs **inside the forked compiler JVM** — ProtoData/Compiler plugins, renderers, option
generators, and the Compiler's own backend — is invisible to JaCoCo/Kover, which instrument only
the Gradle `test` JVM. In the `validation` repo this leaves the two code-generation modules
(`java`, 44 source files; `context`, 25 source files) sitting at **~0% line coverage** even though
every build exercises them heavily against real `.proto` fixtures. The only generator class with
any coverage (`UnsignedIntegerWarnings`, ~82%) has it purely because one spec instantiates it
**in-process**.

The fix is a well-established pattern: **attach the standalone JaCoCo agent to the forked compiler
JVM**, harvest its execution data, and merge it into the consumer's Kover report. The Spine build
already does exactly this for Gradle TestKit worker JVMs (`enableTestKitCoverage()` +
`KoverConfig`); this plan **retargets that same mechanism from TestKit workers to the
`launch*SpineCompiler` tasks**.

The launch tasks are owned by **this repo's** Gradle plugin, so the reusable, upgrade-safe hook
belongs here. Consumers (validation, and any future plugin repo) then opt in.

---

## 2. Background: how Spine measures coverage

- Modules use **Kover with the JaCoCo engine** (`kover { useJacoco(...) }`). Each module's `test`
  task writes `build/kover/bin-reports/test.exec`; Kover renders per-module and a **root
  aggregated** XML report. **Codecov ingests the root report.**
- JaCoCo/Kover instrument **only the JVM that the `test` task runs in**. Any code executed in a
  *different* JVM (a Gradle TestKit worker, or the forked Compiler process) produces **no execution
  data** unless the JaCoCo agent is explicitly attached to that JVM.
- Two reusable helpers already exist in the shared build (`buildSrc` / the `config` submodule) and
  are the direct templates for this work:
  - **`io.spine.gradle.testing.TestKitCoverage.enableTestKitCoverage()`** — resolves the standalone
    JaCoCo agent JAR, injects `-javaagent:…` into Gradle **TestKit worker** JVMs, and collects their
    `.exec` files under `build/jacoco-testkit/`.
  - **`io.spine.gradle.report.coverage.KoverConfig`** — sets up the root aggregation and **merges
    out-of-process `.exec` files into the `total` reports via `additionalBinaryReports`**, both
    per-module and at the root. It already consumes the TestKit exec files
    (`testKitExecFilesProvider` / `rootTestKitExecFilesProvider`).

**Key property of `additionalBinaryReports`:** a Kover report is scoped to the owning project's
classes, so feeding in a `.exec` that contains coverage for *many* classes credits **only this
module's own classes** from it. JaCoCo matches exec data to classes by **class ID** (a hash of the
compiled bytecode), so the merge is correct as long as the instrumented bytecode is identical to
the module's compiled classes.

---

## 3. The problem, precisely

### 3.1 Execution model (verified from the consumer side)

A consumer module that generates code declares, e.g. (`validation/tests/validator/build.gradle.kts`):

```kotlin
buildscript { forceCodegenPlugins() }          // forces io.spine.compiler + core-jvm-compiler
dependencies { spineCompiler(project(":java")) } // the plugin classes to run in the compiler
spine {
    compiler {
        plugins(
            "io.spine.validation.java.JavaValidationPlugin",
            "io.spine.tools.compiler.jvm.style.JavaCodeStyleFormatterPlugin"
        )
    }
}
```

At build time the compiler plugin registers **`JavaExec`** tasks that run the `compiler-fat-cli` in
a **forked JVM**:

- `launchSpineCompiler`
- `launchTestSpineCompiler`
- `launchTestFixturesSpineCompiler`

These are confirmed to be plain `JavaExec` tasks: the consumer helpers `spineCompilerRemoteDebug()`
/ `testSpineCompilerRemoteDebug()` set remote debugging through the **standard Gradle
`JavaExec.debugOptions`** (`check(task is JavaExec)`), which means their **fork options —
including `jvmArgs` — are already reachable**. The plugin classes named in `plugins(...)` (validation's
renderers/generators) load in *this* forked JVM, run against the module's `.proto` files, and emit
generated sources. **None of that execution reaches the `test` JVM's coverage agent.**

### 3.2 Evidence (validation @ HEAD, from Codecov)

| Module | Role | Source files | Coverage |
|---|---|---|---|
| `java` | Java-code generators / renderers (run in compiler JVM) | 44 | **~0%** |
| `context` | ProtoData/Compiler plugin (runs in compiler JVM) | 25 | **~0%** |
| `jvm-runtime` | Runtime library (runs in the test JVM) | 21 | measured |
| — `UnsignedIntegerWarnings.kt` (in `java`) | has a direct in-process spec | — | **~82%** |

Repo total ≈ **12%**. Nearly every 0% file is a generator/renderer/expression-builder that only
ever runs inside the forked compiler JVM. (Separately, validation also suffered a ~6-month coverage
**reporting outage** in late 2025–May 2026 from a broken JaCoCo root-report task dependency; that is
already fixed and is *not* what this plan addresses. This plan is about the **structural** blind
spot, which caps achievable coverage regardless of that outage.)

### 3.3 Why the obvious fixes do **not** apply

- **`SiblingCoverage.creditTestCoverageFrom(contributor)`** (from `tool-base`
  [PR #180](https://github.com/SpineEventEngine/tool-base/pull/180)) re-attributes a sibling
  module's **existing** in-process `test.exec` to another module's per-module report. It requires
  the execution data to **already exist**. For compiler-executed code it does not exist anywhere, so
  there is nothing to credit. (Also note PR #180's own conclusion: `creditTestCoverageFrom` fixed
  `psi`'s *per-module* number 31%→70% but did **not** change the Codecov total, because the root
  report already cross-credits in-process execution. The real Codecov mover in that PR was simply
  **writing in-process unit tests**.)
- **Root Kover aggregation** already cross-credits in-process execution across modules (proven in
  validation: `UnsignedIntegerWarnings` is credited from a spec in a *different* module). There is
  simply no in-process execution of the generators to aggregate.

**Conclusion:** this is an **instrumentation** gap (a JVM with no agent), not an **attribution**
gap. The only way to close it without rewriting every generator as an in-process unit test is to
instrument the forked compiler JVM.

---

## 4. Solution approach

Attach the JaCoCo agent to the forked compiler JVM and merge the result into Kover:

```
compiler JVM (launch*SpineCompiler, JavaExec running compiler-fat-cli)
   └─ -javaagent:org.jacoco.agent.jar=destfile=<build>/jacoco-compiler/<task>.exec,append=true,output=file
        → writes .exec for every class executed (compiler core + plugin classes)

consumer module (KoverConfig)
   └─ kover { reports { total { additionalBinaryReports.add(<task>.exec) } } }
        → credits ONLY this module's own classes (class-ID match) → generators light up
   └─ koverXmlReport / check / koverVerify  dependsOn  launch*SpineCompiler
```

This is the **same mechanism** as `enableTestKitCoverage()`, pointed at a different forked JVM.

### Two decisions that shape the size of the change

1. **Where the JVM-arg hook lives.** If the compiler plugin **preserves consumer-set `jvmArgs`** on
   the launch tasks, the entire feature can be *consumer-side* and this repo's job is only to
   **guarantee** that (add a regression test) plus document/convenience. If the plugin **overwrites
   or manages** `jvmArgs` (e.g. via `setJvmArgs`, `allJvmArgs`, `argumentProviders`, or the Gradle
   Worker API), then this repo must expose a **supported hook** to contribute JVM args / a javaagent.
   → **Phase 0 decides this.**

2. **What is credited.** Only the consumer's plugin classes get credited to the consumer's report
   (class-ID scoping). The Compiler's **own** backend classes are also exercised — this repo can
   optionally capture *that* the same way in its own integration tests (Phase 3).

---

## 5. Why (part of) this belongs in the `compiler` repo

- **Ownership:** `launch*SpineCompiler` are registered by `compiler-gradle-plugin`. A fragile
  consumer-side `-javaagent` hack could break on any compiler upgrade; a supported hook + a
  locking test here is upgrade-safe.
- **Reuse:** validation is the pilot, but every repo that ships Compiler plugins (and the Compiler
  itself) has the identical blind spot. One hook here serves all of them; the consumer glue is then
  upstreamed to `config` once.
- **Compiler self-coverage:** the Compiler's own end-to-end tests spawn the fat-cli; the same hook
  lets this repo measure its backend's real coverage (Phase 3).

---

## 6. Implementation plan

Ordered, with explicit decision gates. Phases 0–1 & 3 are **compiler-repo** work; Phase 2 is the
**consumer (validation)** reference wiring, included so the hook is designed against a real caller.

### Phase 0 — Feasibility spike (compiler repo) — *do this first, it's cheap and decides the design*

Goal: learn how `compiler-gradle-plugin` configures the launch tasks and whether consumer `jvmArgs`
survive.

- [ ] Find where the launch tasks are registered. Grep for: `launchSpineCompiler`,
      `JavaExec`, `register<JavaExec>`, `fatCli` / `compiler-fat-cli`, `jvmArgs`, `setJvmArgs`,
      `allJvmArgs`, `jvmArgumentProviders`, `argumentProviders`, `debugOptions`, `WorkerExecutor`,
      `workerExecutor`, `ProcessForkOptions`, `JavaForkOptions`.
- [ ] Confirm the task type is `JavaExec` in **all** code paths (not the Worker API). The
      remote-debug support strongly implies plain `JavaExec`, but verify there is no worker-based
      alternate path.
- [ ] Confirm the fork runs `compiler-fat-cli` and that the **plugin classes are on the fork's
      classpath verbatim** (not shaded/relocated). If the fat-cli relocates plugin classes, their
      **class IDs won't match** the consumer's compiled classes and the merge will silently credit
      nothing — this is the single biggest risk; check it explicitly.
- [ ] **Manual proof:** in a scratch consumer build (or a compiler integration test), add
      `-javaagent:<jacocoagent.jar>=destfile=…,append=true` to `launchTestSpineCompiler`'s
      `jvmArgs`, run codegen, and confirm (a) a **non-empty** `.exec` is produced and (b) a
      JaCoCo/Kover report generated with that exec as an additional binary report shows **>0%** on a
      plugin class (e.g. a generator).

**Decision gate:**
- **A. `jvmArgs` survive** → Phase 1 is light: add a regression test that locks the behavior in,
  a short doc note, and (optional) a convenience accessor. Most of the feature is Phase 2.
- **B. `jvmArgs` are managed/clobbered, or the task isn't cleanly reachable** → Phase 1 adds an
  explicit, supported hook.

### Phase 1 — Provide a supported coverage / JVM-args hook (compiler repo)

Design a minimal, engine-neutral way for a consumer to contribute JVM args (a javaagent) to the
launch tasks, applied uniformly to `launchSpineCompiler`, `launchTestSpineCompiler`, and
`launchTestFixturesSpineCompiler`.

Options (pick per Phase-0 outcome), least-invasive first:
- [ ] **A. Guarantee + document** that consumer `jvmArgs` / `jvmArgumentProviders` on the launch
      tasks are preserved. Add a test that sets a marker JVM arg and asserts it reaches the fork.
- [ ] **B. First-class DSL**, e.g. on the compiler extension:
      ```kotlin
      spine { compiler { jvmArgumentProviders.add(myAgentProvider) } }
      // or a focused:
      spine { compiler { coverage { agentJar.set(...); destinationDir.set(...); enabled.set(...) } } }
      ```
      Prefer `jvmArgumentProviders` (a `CommandLineArgumentProvider`) over eager `jvmArgs` strings so
      paths resolve lazily and stay configuration-cache-friendly.

Cross-cutting requirements (learned from `TestKitCoverage` — reuse its reasoning):
- [ ] **Opt-in / gated** behind a property or the presence of an agent path, so normal builds pay no
      cost. Mirror how remote-debug is opt-in.
- [ ] **Unique destfile per task and per module** (a module can run several launch variants;
      multi-module builds run many) to avoid clobbering. Suggest
      `build/jacoco-compiler/<taskName>.exec` with `append=true`.
- [ ] **Do not break incremental builds:** when a launch task is `UP-TO-DATE`, no new `.exec` is
      produced. Ensure the consumer's report still finds the previous `.exec` (don't delete it on an
      up-to-date run). `TestKitCoverage` solves the analogous problem with a guarded one-shot wipe +
      marking the producing task non-cacheable; apply the same thinking if this repo owns the
      lifecycle.
- [ ] **Configuration cache & task-output validation friendly** (providers, not eager files;
      declare inputs/outputs correctly).
- [ ] Keep it **engine-agnostic**: emit binary `.exec` (JaCoCo agent's only file output); Kover
      merges binary at the probe level. Do **not** assume XML.

### Phase 2 — Consumer reference wiring (validation) — the pilot that proves the number moves

Implement in validation's `buildSrc` (then upstream to `config` in Phase 4):

- [ ] Add `enableSpineCompilerCoverage()` mirroring `enableTestKitCoverage()`:
      resolve `org.jacoco:org.jacoco.agent:<Jacoco.version>:runtime`, compute a per-module
      `build/jacoco-compiler/` dir, contribute the `-javaagent:` via the Phase-1 hook to the launch
      tasks.
- [ ] Extend `KoverConfig` to feed those `.exec` files into the `total` reports via
      `additionalBinaryReports` (per-module **and** root), exactly like the TestKit exec providers
      already do.
- [ ] Wire ordering: `koverXmlReport` / `check` / `koverVerify` (and their `Cached*` variants)
      **must `dependsOn` the `launch*SpineCompiler` tasks** so the exec exists before a report runs.
      (See `SiblingCoverage.consumesCoverageBinaryReports()` for the task-name predicate to reuse.)
- [ ] Apply to the modules that run codegen (`context`, `java`, and the `tests/*` fixtures that host
      the generated code).
- [ ] **Validate:** `java` and `context` jump from ~0% toward their real exercised percentage; the
      repo total rises materially from ~12%. Capture before/after in the PR.

### Phase 3 — (Optional) Compiler self-coverage (compiler repo)

- [ ] Apply the same hook to the Compiler's **own** integration tests that spawn the fat-cli, so the
      backend's real end-to-end coverage is credited to the compiler modules' reports.

### Phase 4 — Rollout, transparency, and guardrails

- [ ] Upstream `enableSpineCompilerCoverage()` + the `KoverConfig` change into the **`config`**
      submodule so all consumer repos inherit it.
- [ ] **Report codegen coverage transparently.** Crediting build-time codegen execution as
      "coverage" is legitimate for a code generator (the fixtures genuinely exercise these paths) but
      it is *execution* coverage, not assertion-backed. Recommend a **separate Codecov
      flag/component** (`codegen` vs `runtime`) so the two are never conflated. (validation currently
      has `components: []` — nothing configured.)
- [ ] Pair with **targeted in-process unit tests** for logic-heavy generators (the
      `UnsignedFieldWarningSpec` model) — this is what actually moved tool-base's number and it
      catches real bugs.
- [ ] Add a guard so a coverage collapse is noticed early (a Codecov status on absolute drop) —
      validation's 6-month silent outage is the cautionary tale.

---

## 7. Design considerations & risks

| Risk / consideration | Notes & mitigation |
|---|---|
| **Class-ID mismatch (shading/relocation)** | If the fat-cli relocates plugin classes, agent exec won't match the consumer's compiled classes and coverage silently reads 0%. **Verify in Phase 0.** This is the top risk. |
| **Worker API vs JavaExec** | Confirmed `JavaExec` via `debugOptions`; verify no worker-based path exists. Worker API would need a different injection point. |
| **Incremental / UP-TO-DATE launches** | No new `.exec` on up-to-date runs. Don't delete prior exec; keep the producing task's outputs sane. Reuse `TestKitCoverage`'s guarded-wipe + non-cacheable reasoning. |
| **Build cost** | Agent instrumentation slows every codegen launch. Gate behind a property/CI flag; off by default for local dev. |
| **Double counting** | Merge **binary** exec via `additionalBinaryReports` (probe-level), never merge generated XML reports. `KoverConfig` already documents why binary. |
| **Conceptual validity** | Execution coverage ≠ assertion coverage. Report under a distinct flag and complement with unit tests (Phase 4). |
| **Multi-launch collisions** | `launchSpineCompiler` + `launchTestSpineCompiler` + `launchTestFixturesSpineCompiler` may all run per module → unique destfiles. |
| **Configuration cache** | Use lazy providers / `CommandLineArgumentProvider`; avoid capturing `Project` at execution time. |

---

## 8. Pointers & symbols (grep targets)

**In the `compiler` repo (verify these):**
- Plugin id `io.spine.compiler`; artifacts `io.spine.tools:compiler-gradle-plugin`,
  `compiler-fat-cli`, `compiler-backend`, `compiler-jvm`, `compiler-api`, `compiler-gradle-api`.
- Task registration for `launchSpineCompiler` / `launchTestSpineCompiler` /
  `launchTestFixturesSpineCompiler`; the fat-cli fork; any `jvmArgs`/`argumentProviders`/worker use.
- The `spine { compiler { plugins(...) } }` extension implementation.

**Reusable templates (in `validation`/`config` `buildSrc` — copy the pattern):**
- `io.spine.gradle.testing.TestKitCoverage.enableTestKitCoverage()` — agent resolution + injection +
  exec-dir lifecycle (the closest analog; retarget it from TestKit workers to the launch tasks).
- `io.spine.gradle.report.coverage.KoverConfig` — root aggregation + `additionalBinaryReports`
  merge; `testKitExecFilesProvider` / `rootTestKitExecFilesProvider` are the providers to mirror.
- `io.spine.gradle.report.coverage.SiblingCoverage.creditTestCoverageFrom` +
  `consumesCoverageBinaryReports()` — the report/verify task-name predicate for ordering.
- Consumer-side debug hook precedent: `spineCompilerRemoteDebug()` / `setRemoteDebug()` /
  `JavaExec.remoteDebug()` in `buildSrc/src/main/kotlin/BuildExtensions.kt` — shows the launch tasks
  are `JavaExec` and how a consumer reaches them by name.

**Reference material:**
- tool-base coverage PR (attribution helper + the "root already aggregates" lesson):
  https://github.com/SpineEventEngine/tool-base/pull/180
- Codecov (validation), for motivation/before-after:
  https://app.codecov.io/gh/SpineEventEngine/validation

---

## 9. Acceptance criteria

- [ ] Phase 0 conclusion documented: task type, jvmArgs behavior, class-ID match — with the manual
      `.exec` proof attached.
- [ ] A supported way exists for a consumer to attach a javaagent to the launch tasks, covered by a
      **regression test** in this repo.
- [ ] Feature is **off by default**; enabling a flag produces non-empty compiler `.exec`.
- [ ] In validation (pilot): `java` and `context` report **>0%** and the repo total rises materially
      from ~12%, with codegen coverage under its **own flag/component**.
- [ ] Normal (coverage-off) builds show no measurable slowdown and remain configuration-cache clean.
- [ ] Consumer glue upstreamed to `config`; docs updated.
