---
slug: clean-output-only-when-needed
branch: launch-spine-compiler-task
owner: claude
status: in-review
started: 2026-06-11
---

## Goal

`LaunchSpineCompiler` cleans its output directories only when it actually
executes, with the cleanup logic reduced to "delete *our own* output
directories". Incremental builds work without `./gradlew clean`: a no-change
build keeps the task `UP_TO_DATE`, an input change triggers clean
regeneration, and `generated/` is never left empty or stale.

Resolves: https://github.com/SpineEventEngine/compiler/issues/21

## Context

The issue was filed in June 2023 against ProtoData, when the launch task had
no declared inputs/outputs and therefore re-ran — and wiped `generated/` —
on every build. With `generateProto` `UP-TO-DATE`, the request file could be
missing, so the pipeline produced nothing into the just-cleaned target dirs,
breaking compilation. Hence the historic "always `clean build`" requirement.

Much has changed since:

- `LaunchSpineCompiler` is now `@CacheableTask` with declared inputs
  (`sources`, `requestFile`, `settingsDirectory`, both classpaths) and
  outputs (`targets`).
- The request file is a declared output of `GenerateProtoTask`
  (commit `7a1949945`, 2026-06-09), fixing the build-cache restore path.
- Functional tests cover `UP_TO_DATE` on a second run and `FROM_CACHE`
  after `clean` with `--build-cache`.

What remains of the issue today:

1. **The "gymnastics" in `CleanTargetDirs`**
   (`gradle-plugin/.../LaunchSpineCompiler.kt`): the action zips `sources`
   with `targets` positionally and checks `Files.isSameFile` to protect the
   deprecated "overwrite protoc output in place" mode — logic dating back to
   older Protobuf Gradle Plugin layouts. Per the issue comment, it should
   simply clean the task's own output directories.
2. **Descriptor-only gap**: when `protoc` built-ins are off, `sources` is
   absent and the action returns early — target dirs are then *never*
   cleaned, so stale generated files survive re-runs.
3. **Execution-time `project` usage**: the action is an inner class calling
   `project.delete(...)` at execution time — hostile to the configuration
   cache; gets fixed naturally by the rewrite.
4. **No regression tests for the no-`clean` flows** that motivated the
   issue (proto change → rebuild; deleted `generated/` → rebuild; stale
   file removal after a message rename).
5. **Spurious re-runs in consumer repos** (the performance angle): if any
   input — e.g., a settings file written by a consumer plugin — is unstable
   across builds, the task re-runs (clean + full regeneration) every build
   even without changes. Needs verification against a real consumer
   (`core-java`-like) scenario.

## Plan

- [x] Add regression tests (functional, `PluginSpec`) for the no-`clean`
      flows *before* changing the production code:
  - [x] modify a proto file (rename a message) → rebuild without `clean` →
        launch task re-runs, new code present, stale file gone,
        compilation succeeds;
  - [x] delete `generated/` only → rebuild → launch task re-runs and
        restores the output (`generateProto` stays `UP_TO_DATE`);
  - [x] non-proto change only → launch task stays `UP_TO_DATE`,
        `generated/` intact.
- [x] Simplify the cleanup in `LaunchSpineCompiler`:
  - [x] move the cleanup into the task action (`override fun exec()`:
        clean target dirs, then `super.exec()`), dropping
        `requestPreLaunchCleanup()` and the `CleanTargetDirs` inner class;
  - [x] delete only the task's declared output dirs (`targets`) that
        exist; keep a one-line guard skipping any target that canonically
        equals a source dir (protects the deprecated overwrite mode from
        data loss without the zip gymnastics);
  - [x] no early return on absent `sources` — fixes the descriptor-only
        gap (item 2 above);
  - [x] perform deletion without touching `Task.project` at execution
        time (injected `FileSystemOperations`).
- [x] Re-run the full functional test suite; confirm the existing
      `UP_TO_DATE` and `FROM_CACHE` tests still pass (15 tests, 0 failures,
      1 pre-existing `@Disabled` skip).
- [x] Verify the consumer-side story (item 5): added a functional test
      appending a build-script block which rewrites a settings file with
      fixed content on every configuration run; the launch task must stay
      `UP_TO_DATE` on the second build. Producer-side stability of the
      settings content remains a consumer-repo concern.
- [x] Update KDoc of `LaunchSpineCompiler` to state the cleanup contract
      (outputs wiped only on actual execution; up-to-date/cached runs
      leave them intact).
- [x] `pre-pr` checklist (version gate, build, reviewers) before opening
      the PR. PASS — see Log.

## Open questions

1. Is there a scenario **today** (compiler `2.0.0-SNAPSHOT.05x`) where
   `clean build` is still required — e.g., in `core-java`?
   *Answer (2026-06-11): unknown — verify first. The regression tests come
   before any production-code change; whatever they expose gets fixed.*

## Related observations (out of scope)

- `createCleanTask` wires `clean.dependsOn(spineCompilerClean<SS>)` inside
  the lazy `tasks.register {}` block; if nothing realizes the task, the
  dependency may never be established. Worth a separate look.
- **Pre-existing, now fixed in this PR**: the root multi-module Dokka
  aggregation (`:dokkaGeneratePublicationHtml`) failed with
  `missing module-descriptor.json in consolidated Dokka module
  test-env/build/dokka-module/html`. `test-env` calls
  `disableDocumentationTasks()` (`test-env/build.gradle.kts:82`, unchanged
  since 2025-10-29) so its module Dokka tasks are `SKIPPED`, but the root
  consolidation still expected a descriptor from it. Fixed in root
  `build.gradle.kts` by excluding `test-env` from the `dokka(...)`
  aggregation. Carried as a separate commit in this PR.

## Log

- 2026-06-11 — analysed issue #21 and current code; drafted; awaiting
  answer on the current-repro question and plan approval.
- 2026-06-11 — current-repro question answered: "unknown — verify first";
  plan keeps the tests-first shape. Awaiting approval.
- 2026-06-11 — approved; in progress. Three regression tests added to
  `PluginSpec` (reusing the `java-kotlin-test` resource project).
  Version gate: branch already carries `Bump version -> 2.0.0-SNAPSHOT.052`
  (`65e484590`) vs `origin/master` — no new bump needed before
  `publishToMavenLocal`. Baseline run of the new tests against the
  unmodified production code started.
- 2026-06-11 — baseline: all three regression tests PASS against the
  unmodified code (`BUILD SUCCESSFUL`, 1m39s). The 2023-era "always
  `clean build`" chain is closed by the existing input/output
  declarations; the remaining substance is the cleanup simplification.
- 2026-06-11 — production change applied: `exec()` override +
  `cleanTargetDirs()` with injected `FileSystemOperations`;
  `requestPreLaunchCleanup()` and `CleanTargetDirs` removed; class KDoc
  documents the cleanup contract. Added the settings-stability test.
  `:gradle-plugin:check` PASSED — all 15 functional tests green
  (4 new + settings-stability), incl. the existing `UP_TO_DATE`/`FROM_CACHE`.
- 2026-06-11 — pre-pr: three reviewers ran. `spine-code-review`
  APPROVE WITH CHANGES (Should-fix: 5× `createProject("java-kotlin-test")`
  → extracted `createJavaKotlinProject()` helper). `kotlin-engineer`
  APPROVE (applied nits: `!it.list().isNullOrEmpty()`;
  `deleteRecursively() shouldBe true`). `review-docs` APPROVE (applied
  nits: property KDoc noun phrase; "ensures that"; settings-test wording).
  Re-ran `:gradle-plugin:check` + `:gradle-plugin:dokkaGenerate` after the
  fixes — PASS. Full CI-parity `./gradlew build` — PASS (no FAILED tasks).
  Per-module `:gradle-plugin:dokkaGenerate` validates the changed KDoc —
  PASS. The repo-wide `dokkaGenerate` *aggregation* fails on a pre-existing,
  unrelated `test-env` issue (see Related observations); flagged separately,
  not blocking. Wrote `.git/pre-pr.ok` (status=PASS). Changes left
  uncommitted per commit-authorization policy.
