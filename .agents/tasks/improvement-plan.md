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
  `config` and synced; they are listed here only for tracking and must
  NOT be edited in this repository.
- Open questions for a human before some tasks can start are listed in
  the audit report, section 6 (PAT ownership, GA timeline, whether
  `jvm → backend` exposure is intentional, perf budget).

## Plan

Milestone 1 — correctness and hygiene:

- [ ] (S2) Reorder repositories in `settings.gradle.kts` so that
      `mavenLocal()` is not first in `pluginManagement` and
      `dependencyResolutionManagement`; align ordering with
      `core-jvm-compiler`. Effort S, risk Low.
- [ ] (Q2) Replace bare `!!` at user-facing configuration points with
      `checkNotNull`/`requireNotNull` and actionable messages.
      Start with `gradle-plugin/.../Paths.kt:39`. Effort S, risk Low.
- [ ] (S1) `[config]` Remove the scrambled PAT from
      `io/spine/gradle/repo/Repositories.kt`; blocked on audit open
      question 1 (token ownership, GitHub Packages necessity).
- [ ] (Q3) `[config]` Fix or document the silent
      `catch (ignored: Exception)` in
      `buildSrc/.../javascript/task/Check.kt:143`.

Milestone 2 — high-leverage (pre-GA API work):

- [ ] (A1) Narrow `jvm/build.gradle.kts:39` from
      `api(project(":backend"))` to `implementation(...)`; compile
      errors enumerate the truly leaked types; promote genuine contract
      types to `api` module or a small facade. Verify by building
      `core-jvm-compiler` against the locally published snapshot
      (`localPublish` + `tests/`). Leave `testlib`'s re-export as is
      (test harness). Effort L, risk Medium — blocked on audit open
      question 3.
- [ ] (T1) Add direct unit specs for `params` (parameter types,
      file/directory conversions). Kotest assertions, `Spec` suffix,
      stubs not mocks. Effort M, risk Low.
- [ ] (P1) Add a perf smoke signal for the engine (BuildSpeed or a
      timed pipeline run) to PR workflows of this repo — blocked on
      audit open question 4 (perf budget). Effort M, risk Low.

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
- [ ] (T2) `[config]` Write down the coverage policy
      (baseline-no-regression) next to `.codecov.yml`.
- [ ] `[config]` Resolve the 2023-09-22 TODO in
      `buildSrc/.../java/Linters.kt:51`.
- [ ] `[config]` Add `concurrency` groups to build/guard workflows;
      SHA-pin third-party actions.

## Log

- 2026-06-10 — audit completed; report committed as
  `docs/audit-2026-06.md`; plan drafted, awaiting human review of the
  audit's open questions (section 6) before execution starts.
