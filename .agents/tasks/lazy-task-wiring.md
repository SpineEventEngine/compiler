---
slug: lazy-task-wiring
branch: claude/lazy-task-wiring-a189e2
owner: claude
status: in-review
started: 2026-10-29
---

## Goal

The Compiler Gradle plugin creates no tasks by name while the projects are
configured (SpineEventEngine/compiler#98). The dependencies between the launch,
`generate*Proto`, `compile*Java`, `compile*Kotlin`, and `ksp*Kotlin` tasks stay
as they are.

## Context

- Gradle forbids `named(…).configure {}`, `configureEach {}`, and
  `afterEvaluate {}` inside a lazy task configuration action (probed on 9.8.2:
  "cannot be executed in the current context"). So the `generateProto`
  `configureEach` action cannot wire the launch task, as the issue suggests.
  The launch task wires itself to its `generateProto` task instead.
- The `afterEvaluate` fallback for late source sets depended on that wiring.
  `sourceSets.configureEach` replaces it.
- `configureGenerateProtoTasks()` stays eager. Configuring a `GenerateProtoTask`
  rewrites its source set's directories (tool-base `configureSourceSetDirs()`).
  That must happen before protobuf-gradle-plugin's `postConfigure` hands the
  directories to the IDEA plugin. With `configureEach`, reading them creates
  the task mid-iteration and the build fails with `ConcurrentModificationException`.
  The Protobuf plugin creates the tasks there anyway when `idea` is applied.

## Plan

- [x] `TaskLocator`: add `exists()` and `named()`; keep `find()` / `get()`.
- [x] `createTasks()`: `sourceSets.configureEach`; wire compile and KSP tasks with
      `tasks.named { … }.configureEach { dependsOn(launchTask) }`.
- [x] `configureGenerateProtoTasks()`: the action configures only the
      `GenerateProtoTask` itself; iteration stays eager (see Context).
- [x] `LaunchSpineCompiler.setDependencies()`: depend on `generate*Proto` by provider;
      drop `findJavaCompileFor()` / `findKotlinCompileFor()`.
- [x] Tests: no tasks created at configuration time; dependencies still set;
      late source set and late KSP task; `generateProto` gets the Compiler plugin
      and the source set gets the generated directories; `idea` applied.
- [x] `./gradlew build`; measure a real consumer with the task stats.
      `ksp-test` fixture, `./gradlew help`: `.081` created 14 of 53 tasks, `.082`
      created 6 (the `launch*`, `compile*Java`, `compile*Kotlin`, `ksp*Kotlin` tasks
      are no longer created). In `tests/`, the same tasks are now created first by
      `buildSrc`'s `DokkaExts.disableDocumentationTasks` (SpineEventEngine/config#820).

## Log

- 2026-10-29 — drafted and started.
- 2026-10-29 — `configureEach` for `generateProto` broke `integrationTest` (CME with
  `idea`); reverted that call site to eager iteration, added a regression test.
- 2026-10-29 — `gradle-review`: no blockers. Added tests for the `clean` wiring,
  `protoSourceDirs` contents, and the Kotlin compile task; `createCleanTask()` takes
  the launch task provider. Not staged for commit: the version bump and dependency
  reports (separate commits per the version policy).
- 2026-10-29 — pre-PR review (`spine-code-review`, `kotlin-engineer`, `review-docs`): no
  Must-fix. Added a test creating each task as soon as it is registered, made
  `consumeProtoFrom()` private, and fixed KDoc wording.
