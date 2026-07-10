---
slug: fix-launch-task-exec-time-config
branch: master              # working tree only; the user decides branch/commit
owner: claude
status: in-review
started: 2026-07-10
---

## Goal

`LaunchSpineCompiler` no longer mutates task state at execution time, so
consumer builds (e.g. `delivery-server`) stop seeing the Gradle 9.6 deprecations
that become errors in Gradle 10/11:

1. *"Changing property value of task '…:launchSpineCompiler' property 'mainClass'
   at execution time"* (error in Gradle 11).
2. *"Invocation of Task.dependsOn at execution time"* (error in Gradle 10,
   configuration-cache incompatible).

## Context

Both warnings originate from the `doFirst { compileCommandLine();
createParametersFile() }` block installed by `applyDefaults()` in
`gradle-plugin/src/main/kotlin/io/spine/tools/compiler/gradle/plugin/LaunchSpineCompiler.kt`:

- `compileCommandLine()` sets `mainClass`, `classpath`, and `args` inside a task
  action. `mainClass` is a tracked `Property` — Gradle nags on it now; the rest
  will follow.
- `createParametersFile()` traverses `dependsOn` at execution time to locate
  the `GenerateProtoTask`.

None of the deferred values actually require execution-time assembly:
`mainClass` is the constant `CLI_APP_CLASS`; the classpath consists of two lazy
`Configuration`s; the only argument pair is `--params <file>` whose path is
fully known once the source-set name is fixed in `applyDefaults()`.

Observed in `delivery-server` `build/reports/problems/problems-report.html`
(build `:admin-server:build`, Gradle 9.6.1), task `:grpc-api:launchSpineCompiler`.

## Plan

- [x] `LaunchSpineCompiler.kt`
  - [x] Set `mainClass.set(CLI_APP_CLASS)` in the `init` block (constant).
  - [x] Add `classpath(...)` for the two configurations in `applyDefaults()`
        (configuration time; resolution stays lazy).
  - [x] Register the `--params <file>` pair via `argumentProviders` in
        `applyDefaults()` — evaluated when the command line is built, without
        mutating `args`, and (as today) without entering the build-cache key,
        keeping cached outputs relocatable.
  - [x] Add `@get:Internal internal abstract val protoSourceDirs:
        ConfigurableFileCollection` — explicit carrier for the proto source
        dirs, replacing the `dependsOn` traversal. `Internal` because the proto
        content is already fingerprinted via `requestFile`.
  - [x] Add `consumeProtoFrom(GenerateProtoTask)` helper: `dependsOn(task)` +
        `protoSourceDirs.from(task.sourceDirs)`.
  - [x] `createParametersFile()`: read `protoSourceDirs` instead of
        `dependsOn.first { it is GenerateProtoTask }`.
  - [x] ~~Call `createParametersFile()` from the `exec()` override~~ — reverted;
        the write stays a `doFirst` action (see Log for the classloader
        constraint). `compileCommandLine()` deleted as planned.
- [x] `Plugin.kt`
  - [x] `handleLaunchTaskDependency()`: use `consumeProtoFrom(...)` in both
        branches (existing task / `afterEvaluate` creation).
- [x] Verify: full `./gradlew build` green (all module tests + functional
      tests); regression test `PluginSpec."not mutate the launch task state
      at execution time"` added (runs `launch-test` with `--warning-mode=all`
      and asserts the two messages are absent); version bumped to
      `2.0.0-SNAPSHOT.062`; dependency reports regenerated; A/B-proven with
      a standalone consumer on Gradle 9.6.1: plugin `.060` emits both
      warnings, `.062` emits none, launch task executed in both runs.

## Log

- 2026-07-10 — analysed delivery-server problems report; drafted plan; executing.
- 2026-07-10 — both files edited; note: the `--info` command-line log from
  `compileCommandLine()` is gone; `createParametersFile()` still logs the
  parameters file path. Build + `spine-code-review`/`kotlin-engineer`
  reviews running.
- 2026-07-10 — functional tests failed (14/17): moving `createParametersFile()`
  into the `exec()` override broke Spine `KnownTypes` loading —
  `PipelineParameters.toJson()` resolves `desc.ref` resources via the thread
  context classloader, which Gradle sets to the plugin classloader only
  around `doFirst`/`doLast` actions, not around the `@TaskAction` method.
  Fix: the parameters-file write stays a `doFirst` action (a file-system
  side effect is not a task-state mutation, so no deprecation); constraint
  recorded in a code comment at the call site. Re-running tests.
- 2026-07-10 — all green. Reviews: `kotlin-engineer` APPROVE,
  `spine-code-review` APPROVE WITH CHANGES — applied: `check` with curated
  message instead of raw cast in `handleLaunchTaskDependency`, KDoc precision,
  version bump, deprecation regression test in `PluginSpec`. A/B verification
  via a scratchpad consumer project (mavenLocal plugin `.060` vs `.062`,
  `--warning-mode=all`): warnings present before, absent after.
  Working tree ready for review; not committed (no commit authorization).
