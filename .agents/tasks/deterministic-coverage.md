---
slug: deterministic-coverage
branch: deterministic-coverage
owner: claude
status: in-progress
started: 2026-10-16
---

## Goal

Make the coverage that Codecov receives for `api` classes deterministic, so that
`codecov/project` (threshold 0.05%) stops failing pull requests at random.

## Context

Between CI runs of the same tree, Codecov credited some `api` files with full coverage
or with none: `FieldTypeExts.kt`, `FilePatterns.kt`, `TypeExts.kt`, `DescriptorExts.kt`,
`value/Options.kt`, `MessageTypeDependencies.kt`, and `ValueConverter.kt` (up to 182 hits).
The tests ran each time; the hits were dropped from the root Kover report.

Root cause, reproduced locally:

1. `tests/settings.gradle.kts` had `includeBuild("..")`. The `integrationTest` build
   substituted the `compiler-*` artifacts with the root projects and ran their tasks
   (`launchSpineCompiler`, `kspKotlin`, `compileKotlin`, `jar`) in the root project
   directories, after every `test` task and, in practice, before `koverXmlReport`.
2. `:api:launchSpineCompiler` is never up to date in a second invocation: the Validation
   Gradle plugin writes `JavaValidationRenderer.pb.json` into the task's `settingsDirectory`
   input in a `doFirst` action, after Gradle has fingerprinted the inputs.
3. The rerun regenerates `api/generated/**`. The Validation code generator names the fields
   holding compiled regular expressions with `SecureRandomString`, so the sources really
   differ, and the Kotlin incremental compiler recompiles the hand-written files that use
   the regenerated classes. The recompiled class files differ only in constant-pool layout,
   yet that changes their JaCoCo class IDs.
4. `koverXmlReport` then reads class files whose IDs match no execution data, and reports
   them as uncovered. Which classes are hit depends on what the rerun regenerates.

Local evidence (clean build of `2.0.0-SNAPSHOT.076` with `includeBuild("..")` still in place):
the exec files of all test tasks matched all 249 executed `api` classes as compiled before
`integrationTest` ran, but 5 of them (`FieldTypes`, `FilePatterns`, `Types`, `DescriptorExtsKt`,
`OptionValue`) did not match the class files the report analyzed. The report showed
`FieldTypeExts.kt` with 0 of 73 lines covered, and `FilePatterns.kt` with 0 of 9.

## Plan

- [x] Bump the version to `2.0.0-SNAPSHOT.076` (`.075` is taken by PR #90 and another branch).
- [x] Drop `includeBuild("..")`: the `tests` build consumes the artifacts that `localPublish`
      publishes to Maven Local, which `integrationTest` already depends on.
- [x] Resolve the Compiler of the version under development from Maven Local only
      (`exclusiveContent`), so that a remote repository with the same version never
      serves it instead. Repository order alone does not do it: the `tests` root script
      adds the remote repositories to the subprojects before their own scripts run.
- [x] Update `tests/README.md`.
- [ ] Verify two consecutive CI coverage uploads give the same totals for the files
      listed in Context.

Follow-ups in the `validation` repository, out of scope here: deterministic generated
identifiers; writing the renderer settings before input fingerprinting.

## Log

- 2026-10-16 22:30 — root cause reproduced locally; fix applied.
- 2026-10-16 22:45 — fixed clean builds, with and without the build cache: the nested build
  runs no root tasks, all 249 executed `api` classes match, and both Kover reports are
  identical file by file.
- 2026-10-16 23:05 — review: Maven Local was not first for subproject repositories; switched
  to `exclusiveContent` scoped to the version under development. Verified: with that version
  absent from Maven Local, resolution fails instead of falling back to the registry.
