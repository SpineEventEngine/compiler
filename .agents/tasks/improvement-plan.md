---
slug: improvement-plan
branch: claude/loving-hypatia-tmv0w1
owner: claude
status: draft
started: 2026-06-10
---

## Goal

Close the findings of the 2026-06 repository audit
([`docs/audit-2026-06.md`](../../docs/audit-2026-06.md)) that are owned
by this repository: tighten the public API surface before the 2.0.0 GA,
remove user-facing failure footguns, add direct tests for untested
modules, and add user-facing documentation. Finding IDs below (S2, A1,
…) refer to the audit report.

## Context

- The audit found no Critical issues; this plan covers Medium/Low items
  with the highest leverage.
- Tasks marked `[config]` live in files vendored from the
  `SpineEventEngine/config` submodule (e.g. `buildSrc/**`,
  `.github/workflows/**`, `.codecov.yml`). They must be fixed in
  `config` and synced; they are NOT editable in this repository. They
  are consolidated in
  [config#691](https://github.com/SpineEventEngine/config/issues/691)
  and listed under "Delegated to `config`" below, not in the milestone
  checklists.
- Open questions for a human before some tasks can start are listed in
  the audit report, section 6 (PAT ownership, GA timeline, whether
  `jvm → backend` exposure is intentional, perf budget).

## Plan

Milestone 1 — correctness and hygiene:

- [x] (S2) Reorder repositories in `settings.gradle.kts` so that
      `mavenLocal()` is not first in `pluginManagement` and
      `dependencyResolutionManagement`; align ordering with
      `core-jvm-compiler`. Effort S, risk Low.
- [x] (Q2) Replace bare `!!` at user-facing configuration points with
      `checkNotNull`/`requireNotNull` and actionable messages.
      Start with `gradle-plugin/.../Paths.kt:39`. Effort S, risk Low.

Milestone 2 — high-leverage (pre-GA API work):

- [x] (A1) `jvm/build.gradle.kts:40` — narrowing
      `api(project(":backend"))` to `implementation(...)` is not viable:
      the `api` edge is intentional so downstream consumers can reach
      the engine's `Pipeline` and `CodeGenerationContext` API. Resolved
      via the audit's "or an ADR records why it must" done-signal by
      documenting the edge in place with a `because(...)` rationale (it
      previously carried none). Answers audit open question 3 — the
      `jvm → backend` exposure is intentional.
- [x] (T1) Added direct unit specs for `params` (parameter types,
      file/directory conversions) — 5 `Spec` files, 19 cases, Kotest
      assertions, stubs not mocks. Line coverage 0% → 94.5%; the only
      uncovered lines were the unreachable field comparisons in
      `Parameter.equals()` (68–70), since removed (see changelog).
      Effort M, risk Low.
- [x] (P1) Added a perf **smoke signal** (not a gate) for the engine: a
      timed cold `Pipeline` run (`PipelineSmokeSpec`, tagged `performance`)
      run by a dedicated `:backend:performanceTest` task from a new
      repo-specific `performance-test.yml` PR workflow. It fails only on a
      coarse hang ceiling (a deadlock/pathology guard), so it asserts no
      perf budget — sidestepping audit open question 4 instead of waiting
      on it. BuildSpeed is deliberately avoided. Effort M, risk Low.

Milestone 3 — polish:

- [ ] (Doc1) Getting-started page: apply the Gradle plugin → generate →
      inspect output; plus a "writing a compiler plugin" guide for
      `api`/`gradle-api` consumers. Use the `writer` skill. Effort L.
      Best done after the A1 surface settles.
- [ ] (A4) Document the process-exit contract of
      `Compilation.error()`/`PluginFactory` (KDoc: production exits the
      forked JVM, tests throw `Compilation.Error`), or refactor to
      exception-first with the exit at the CLI boundary. Effort M,
      risk Medium.

## Delegated to `config`

These audit findings touch files vendored from the
`SpineEventEngine/config` submodule (`buildSrc/**`, `.codecov.yml`, and
the workflow templates under `.github-workflows/`). They must be fixed in
`config` and synced, and are intentionally **not** actionable in this
repository. All five are consolidated in
[config#691](https://github.com/SpineEventEngine/config/issues/691):

- (S1) Remove the scrambled GitHub PAT from
  `buildSrc/.../repo/Repositories.kt` — blocked on audit open question 1
  (token ownership, GitHub Packages necessity).
- (Q3) Document or fix the silent `catch (ignored: Exception)` in
  `buildSrc/.../javascript/task/Check.kt:143`.
- (Q1 drift) Resolve the 2023-09-22 TODO in
  `buildSrc/.../java/Linters.kt:51`.
- (T2) Write down the baseline-no-regression coverage policy next to
  `.codecov.yml`.
- (D1 + S3) Add `concurrency` groups to the build/guard workflow
  templates and SHA-pin third-party actions.

## Log

- 2026-06-10 — audit completed; report committed as
  `docs/audit-2026-06.md`; plan drafted, awaiting human review of the
  audit's open questions (section 6) before execution starts.
- 2026-06-15 — (S2) done. Reordered `settings.gradle.kts` to
  `gradlePluginPortal()` → `mavenLocal()` → `mavenCentral()` in both
  `pluginManagement` and `dependencyResolutionManagement`, matching
  `core-jvm-compiler`. Verified with `:params:dependencies` — build
  configures and Spine `-SNAPSHOT` artifacts still resolve from
  `mavenLocal`, so the sibling-repo snapshot workflow is intact.
- 2026-06-15 — (Q2) done. Both bare `protobufExtension!!` sites in
  `gradle-plugin` (`Paths.kt` `protocOutputDir`, `Plugin.kt`
  `setProtocArtifact`) now go through a shared
  `Project.protobufExtensionOrFail()` helper that uses `checkNotNull`
  with an actionable message naming the missing `com.google.protobuf`
  plugin and the project path. Verified: `:gradle-plugin` compiles,
  tests 5/5 pass, `dokkaGenerate` and `detekt` clean.
- 2026-06-15 — `[config]` items moved out of the milestone checklists
  into a new "Delegated to `config`" section and consolidated into a
  single tracking issue,
  [config#691](https://github.com/SpineEventEngine/config/issues/691):
  S1 (scrambled PAT), Q3 (silent npm-audit catch), Q1 drift
  (2023-09-22 `Linters.kt` TODO), T2 (coverage policy), and D1+S3
  (workflow `concurrency` groups + SHA-pinned actions). They are fixed
  in `config` and synced, not in this repo. Before filing, confirmed the
  three `buildSrc` files are byte-identical between `config` and this
  repo, and verified every cited path/line against the `config` working
  copy (workflow templates live in `config`'s `.github-workflows/`).
- 2026-06-15 — (A1) resolved by documentation, not narrowing. Concluded
  `jvm`'s `api(project(":backend"))` cannot be reduced to
  `implementation(...)`: the engine's `Pipeline` and
  `CodeGenerationContext` are part of the contract downstream consumers
  program against, so the `api` edge is intentional. Added a
  `because(...)` rationale at `jvm/build.gradle.kts:40-45` stating this
  — closing A1 through the audit's sanctioned "ADR records why it must"
  path and answering audit open question 3 (yes, intentional).
- 2026-06-15 — (T1) done via the `raise-coverage` skill on `:params`.
  Added 5 Kotlin `Spec` files (19 cases) under
  `params/src/test/kotlin/io/spine/tools/compiler/params/`:
  `ParameterSpec` (covers `Parameter` + the three `CommandLineInterface`
  param objects via Guava `EqualsTester`), `ParametersDirectorySpec`,
  `WorkingDirectorySpec`, `RequestDirectorySpec`, and
  `CodeGeneratorRequestFileSpec`. Kover line coverage for `:params` went
  0% → 94.5% (52/55 lines); all 19 tests pass. Remaining uncovered code
  is non-actionable: the unreachable field comparisons in
  `Parameter.equals()` (68–70, dead because the `super.equals` identity
  check short-circuits) and two trivial unused constant getters
  (`ParametersDirectory.DEFAULT_FORMAT`, `Parameter.ps`). Branch is
  already version-bumped (`2.0.0-SNAPSHOT.053`).
- 2026-06-15 — (P1) done as a **smoke signal, not a gate** — the chosen
  shape avoids running BuildSpeed as a perf gate on PRs. Added
  `backend/.../backend/perf/PipelineSmokeSpec.kt`: a `@Tag("performance")`
  test that times one cold `Pipeline` run over the `test-env` `doctor.proto`
  fixture (reusing the `PipelineSpec` construction), logs the elapsed time,
  and asserts only that it stays under a deliberately generous 60s hang
  ceiling. Because it asserts no perf budget, audit open question 4 does
  **not** block it (Q4 only gates a workflow that *asserts* a budget). Wired
  via a dedicated `:backend:performanceTest` task in `backend/build.gradle.kts`
  (the `performance` tag is excluded from the normal `test`); the task is not
  in `check`/`build`. Runs on PRs through a new **repo-specific**
  `.github/workflows/performance-test.yml` — confirmed `performance-test.yml`
  is not a `config` template (it is git-tracked directly in
  `core-jvm-compiler`), so it is introduced here, not delegated to `config`.
  BuildSpeed stays the downstream end-to-end measure; this is the upstream
  early-warning signal where the engine lives.
- 2026-06-15 — Removed the dead `Parameter.equals()`/`hashCode()` overrides
  that the T1 entry flagged as the only non-actionable uncovered lines.
  `Parameter` is `sealed` with three `object` subtypes, so identity equality
  (default `Any`) is the sole achievable behavior; the custom `equals()` was
  unreachable past its `super.equals` identity short-circuit (former lines
  68–70). Dropped both overrides — kept `toString()` returning the long name
  for readable logs — removed the now-invalid `hashCode` spec case (19 → 18
  `:params` cases), and `ParameterSpec`'s `EqualsTester` now asserts default
  identity semantics. Bumped `Parameter.kt`'s copyright to 2026. Verified with
  `./gradlew :params:test :params:detekt`: 18 tests pass, detekt clean. Branch
  stays version-bumped (`2.0.0-SNAPSHOT.053`); no re-bump needed.
