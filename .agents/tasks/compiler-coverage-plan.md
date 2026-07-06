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

| Module                                     | Role                                                   | Source files | Coverage |
|--------------------------------------------|--------------------------------------------------------|--------------|----------|
| `java`                                     | Java-code generators / renderers (run in compiler JVM) | 44           | **~0%**  |
| `context`                                  | ProtoData/Compiler plugin (runs in compiler JVM)       | 25           | **~0%**  |
| `jvm-runtime`                              | Runtime library (runs in the test JVM)                 | 21           | measured |
| — `UnsignedIntegerWarnings.kt` (in `java`) | has a direct in-process spec                           | —            | **~82%** |

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

### Phase 0 — Feasibility spike (compiler repo) — ✅ DONE (2026-07-06)

Goal: learn how `compiler-gradle-plugin` configures the launch tasks and whether consumer `jvmArgs`
survive.

- [x] Find where the launch tasks are registered. → `Project.createLaunchTask()` in
      `gradle-plugin/src/main/kotlin/io/spine/tools/compiler/gradle/plugin/Plugin.kt` registers
      `LaunchSpineCompiler` per source set (name built by `CompilerTask.nameFor()`:
      `launch[<SourceSet>]SpineCompiler`). Both code paths — the eager `createTasks()` loop and
      the `afterEvaluate` fallback in `handleLaunchTaskDependency()` for late-added source sets —
      go through `createLaunchTask()`.
- [x] Confirm the task type is `JavaExec` in **all** code paths. → Confirmed:
      `LaunchSpineCompiler : JavaExec()` (`LaunchSpineCompiler.kt`). No Worker API anywhere
      (`WorkerExecutor`/`workerExecutor`: zero hits). **`jvmArgs` survive:** the task's `init`
      block calls the *additive* `jvmArgs(...)` overload (four `--add-opens` for the Palantir
      formatter); `compileCommandLine()` (a `doFirst`) touches only `classpath`, `mainClass`, and
      program `args`. No `setJvmArgs`/`allJvmArgs`/`jvmArgumentProviders` manipulation anywhere in
      the plugin.
- [x] Confirm the fork runs the fat CLI and plugin classes are on the fork's classpath verbatim. →
      The fork's classpath is two configurations, side by side (`compileCommandLine()`):
      `spineCompilerRawArtifact` (= `io.spine.tools:compiler-cli-all`, the shadow JAR of the `cli`
      module) and `spineCompiler` (the user classpath with consumer plugin classes as their
      **original artifacts**). The shadow config (`ShadowJarExts.setup()`) does **no `relocate()`**
      — only service-file merging and first-copy-wins dedup. Consumer classes never enter the fat
      JAR at all, so **class IDs match by construction**.
- [x] **Manual proof:** captured as the permanent functional test `LaunchTaskCoverageSpec`
      (see Phase 1). Its fixture `coverage-agent-test` attaches
      `-javaagent:<jacocoagent>=destfile=build/jacoco-compiler/<task>.exec,append=true` via
      `jvmArgumentProviders`, runs codegen, and asserts: (a) a non-empty `.exec`;
      (b) JaCoCo `Analyzer` over the very `compiler-test-env` jar the fork loaded reports
      **covered lines > 0** on `UnderscorePrefixRenderer` (class-ID match proven);
      (c) an `UP-TO-DATE` re-launch leaves the `.exec` intact.
      **Executed green** on 2026-07-06: `./gradlew :gradle-plugin:functionalTest
      --tests "...LaunchTaskCoverageSpec"` → `BUILD SUCCESSFUL` (also
      `LaunchTaskJvmArgsSpec` via `:gradle-plugin:test`).

**Decision gate: A.** Consumer `jvmArgs`/`jvmArgumentProviders` survive; the whole feature is
consumer-side wiring. This repo adds the regression lock + docs (Phase 1); no new DSL is needed.
- **A. `jvmArgs` survive** → Phase 1 is light: add a regression test that locks the behavior in,
  a short doc note, and (optional) a convenience accessor. Most of the feature is Phase 2.
- ~~B. `jvmArgs` are managed/clobbered~~ — not the case.

Additional facts for Phase 2 (consumer wiring), verified here:
- `LaunchSpineCompiler` is `@CacheableTask`; its inputs/outputs are declared explicitly and do
  **not** include JVM args, so toggling the agent neither invalidates up-to-dateness nor pollutes
  cache keys. Consequences: (1) an `UP-TO-DATE`/`FROM-CACHE` launch produces no fresh `.exec` —
  keep the previous file (do not wire a `dependsOn`-style cleaner); (2) a full re-measure needs
  `clean` or `--rerun-tasks`.
- The `.exec` must **not** be declared a task output (it would leak into the build cache key
  space and break cache-hit restores; same reasoning as `TestKitCoverage` step 4).
- The user classpath configuration is named `spineCompiler`
  (`Names.USER_CLASSPATH_CONFIGURATION`); the launch tasks are found by type
  `io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler` or by name via
  `io.spine.tools.compiler.gradle.api.CompilerTask`.

### Phase 1 — Provide a supported coverage / JVM-args hook (compiler repo) — ✅ DONE (option A)

Design a minimal, engine-neutral way for a consumer to contribute JVM args (a javaagent) to the
launch tasks, applied uniformly to `launchSpineCompiler`, `launchTestSpineCompiler`, and
`launchTestFixturesSpineCompiler`.

Implemented (option A per the Phase-0 gate):
- [x] **A. Guarantee + document** that consumer `jvmArgs` / `jvmArgumentProviders` on the launch
      tasks are preserved:
      - `LaunchTaskJvmArgsSpec` (`gradle-plugin/src/test`) — fast in-process lock: the launch task
        is a plain `JavaExec`; a consumer-added JVM arg coexists with the task's own
        `--add-opens` defaults.
      - `LaunchTaskCoverageSpec` (`gradle-plugin/src/functionalTest`) + the `coverage-agent-test`
        fixture — end-to-end lock: a real JaCoCo agent attached via `jvmArgumentProviders`
        reaches the forked JVM, records `UnderscorePrefixRenderer` execution, the data matches
        the fork's own jars (class IDs), and an `UP-TO-DATE` launch keeps the `.exec`.
        The fixture doubles as the reference wiring for consumers (Phase 2).
      - KDoc on `LaunchSpineCompiler` now declares the fork options part of the task's public
        contract (“Attaching instrumentation to the forked JVM”).
- **B. First-class DSL** — deliberately **not** added: standard `JavaExec` fork options are
      the hook; a DSL would duplicate them. Revisit only if the launch tasks ever stop
      being `JavaExec`. Consumers should prefer `jvmArgumentProviders`
      (a `CommandLineArgumentProvider`) over eager `jvmArgs` strings so paths resolve lazily
      and stay configuration-cache-friendly.

Cross-cutting requirements (learned from `TestKitCoverage`). Under option A the lifecycle is
owned by the **consumer** helper (`enableSpineCompilerCoverage()`, Phase 2) — these become its
requirements; the fixture demonstrates the per-task destfile and lazy-provider parts:
- [ ] **Opt-in / gated** behind a property or the presence of an agent path, so normal builds pay no
      cost. Mirror how remote-debug is opt-in. *(Consumer-side, Phase 2.)*
- [x] **Unique destfile per task and per module** — the fixture uses
      `build/jacoco-compiler/<taskName>.exec` with `append=true`.
- [x] **Do not break incremental builds:** when a launch task is `UP-TO-DATE`, no new `.exec` is
      produced and the previous one is kept — asserted by `LaunchTaskCoverageSpec`. Do **not**
      declare the `.exec` a task output, and do not wipe it via a `dependsOn` cleaner (guarded
      one-shot wipe if needed, per `TestKitCoverage`). *(Wipe lifecycle: consumer-side, Phase 2.)*
- [x] **Configuration cache & task-output validation friendly**: the reference wiring uses
      a `CommandLineArgumentProvider` (lazy agent path & destfile), no eager files
      at configuration time.
- [x] Keep it **engine-agnostic**: the agent emits binary `.exec`; nothing here assumes XML.

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

| Risk / consideration                       | Notes & mitigation                                                                                                                                                           |
|--------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Class-ID mismatch (shading/relocation)** | If the fat-cli relocates plugin classes, agent exec won't match the consumer's compiled classes and coverage silently reads 0%. **Verify in Phase 0.** This is the top risk. |
| **Worker API vs JavaExec**                 | Confirmed `JavaExec` via `debugOptions`; verify no worker-based path exists. Worker API would need a different injection point.                                              |
| **Incremental / UP-TO-DATE launches**      | No new `.exec` on up-to-date runs. Don't delete prior exec; keep the producing task's outputs sane. Reuse `TestKitCoverage`'s guarded-wipe + non-cacheable reasoning.        |
| **Build cost**                             | Agent instrumentation slows every codegen launch. Gate behind a property/CI flag; off by default for local dev.                                                              |
| **Double counting**                        | Merge **binary** exec via `additionalBinaryReports` (probe-level), never merge generated XML reports. `KoverConfig` already documents why binary.                            |
| **Conceptual validity**                    | Execution coverage ≠ assertion coverage. Report under a distinct flag and complement with unit tests (Phase 4).                                                              |
| **Multi-launch collisions**                | `launchSpineCompiler` + `launchTestSpineCompiler` + `launchTestFixturesSpineCompiler` may all run per module → unique destfiles.                                             |
| **Configuration cache**                    | Use lazy providers / `CommandLineArgumentProvider`; avoid capturing `Project` at execution time.                                                                             |

---

## 8. Pointers & symbols (grep targets)

**In the `compiler` repo (verified):**
- Plugin id `io.spine.compiler`; artifacts `io.spine.tools:compiler-gradle-plugin`,
  `compiler-backend`, `compiler-jvm`, `compiler-api`, `compiler-gradle-api`. The fat CLI artifact
  is **`io.spine.tools:compiler-cli-all`** (shadow JAR of the `cli` module; the plan's working
  name `compiler-fat-cli` refers to it — see `Artifacts.fatCli()` in `gradle-api`).
- Task registration: `Plugin.kt` → `createLaunchTask()`; task class `LaunchSpineCompiler`
  (`JavaExec`); fork classpath assembled in `LaunchSpineCompiler.compileCommandLine()`.
- The `spine { compiler { plugins(...) } }` extension implementation: `Extension` /
  `CompilerDslSpec` in `gradle-plugin`.

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

- [x] Phase 0 conclusion documented: task type, jvmArgs behavior, class-ID match — with the
      `.exec` proof captured as `LaunchTaskCoverageSpec` (see Phase 0 section above).
- [x] A supported way exists for a consumer to attach a javaagent to the launch tasks
      (standard `JavaExec` fork options, declared part of the public contract in
      the `LaunchSpineCompiler` KDoc), covered by **regression tests** in this
      repo (`LaunchTaskJvmArgsSpec`, `LaunchTaskCoverageSpec`).
- [x] Feature is **off by default** — nothing in the plugin changes unless a consumer wires the
      agent; the `coverage-agent-test` fixture shows the opt-in wiring producing a non-empty
      `.exec`. *(The consumer-side flag gating lands with Phase 2.)*
- [ ] In validation (pilot): `java` and `context` report **>0%** and the repo total rises materially
      from ~12%, with codegen coverage under its **own flag/component**.
      *(Phase 2 — validation repo.)*
- [ ] Normal (coverage-off) builds show no measurable slowdown and remain configuration-cache clean.
      *(Verify in the Phase 2 pilot; this repo adds no always-on cost.)*
- [ ] Consumer glue upstreamed to `config`; docs updated. *(Phase 4.)*
