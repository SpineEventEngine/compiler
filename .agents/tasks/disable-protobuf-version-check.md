---
slug: disable-protobuf-version-check
branch: disable-protobuf-version-check
owner: claude
status: in-review
started: 2026-10-19
---

## Goal

Users can turn off the Protobuf runtime/gencode version check in the process
of the Compiler with a DSL call instead of exporting an environment variable:

```kotlin
spine {
    compiler {
        disableProtobufVersionCheck.set(true)
    }
}
```

When set to `true`, every `LaunchSpineCompiler` task passes
`TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK=true` to the forked Compiler JVM.

## Context

- `protobuf-java` 4.x validates the gencode version against the runtime in
  `RuntimeVersion.validateProtobufGencodeVersion()` and skips the check only
  when `System.getenv("TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK")` is `"true"`.
  The Compiler fork is where a user classpath (plugins built with another
  `protoc`) meets the Compiler's Protobuf runtime, so it is the process where
  users hit the check.
- Today the only ways are exporting the variable for the Gradle daemon, or
  `tasks.withType<LaunchSpineCompiler> { environment(...) }`. The new property
  is a convenience shortcut for the latter.
- Gradle 9.8.1 (this branch's wrapper; also checked 9.7.1)
  `DefaultProcessForkOptions.getActualEnvironment()` converts values
  with `String.valueOf()` — no `Provider` unpacking. `JavaExec.getEnvironment()`
  is marked `@ToBeReplacedByLazyProperty`; once it is lazy, move the write to
  configuration time. Launch tasks are realized
  while the plugin is applied, before the user's `compiler { }` block runs, so
  the flag must be read at execution time.
- `JavaExec.getEnvironment()` is a plain `@Internal` map (not a tracked
  `Property`), so setting it in the `exec()` override does not trip the
  "changing property value at execution time" deprecation addressed by
  `fix-launch-task-exec-time-config`.
- `Extension` is the only implementation of `CompilerSettings` across Spine repos
  (`core-jvm-compiler` only calls it), so adding an interface member is safe.
- Out of scope: the Gradle daemon and the `protoc` plugin processes. Their
  environment cannot be changed from the build script; users still export the
  variable for those.
- After #90 (`align-protobuf-runtime`), the fork runs on the highest Protobuf runtime
  requested by the CLI and by `spineCompiler`, so a newer plugin runtime no longer
  fails. The setting remains for builds that force an older runtime (e.g. via `force`).
- Not covered end-to-end: the functional tests export the variable for every
  fixture build (`RunnerTuning.tuneRunner()`). The `protobuf-runtime-test` fixture
  from #90 removes it in the fork, so a follow-up can force an older runtime there
  and show the flag fixing the failure. The helper is unit-tested here.

## Plan

- [x] `CompilerSettings` (`gradle-api`): add
      `public val disableProtobufVersionCheck: Property<Boolean>` with KDoc.
- [x] `Extension` (`gradle-plugin`): implement it with `convention(false)`.
- [x] `LaunchSpineCompiler`:
  - [x] `@get:Internal internal abstract val disableProtobufVersionCheck:
        Property<Boolean>` — internal because it does not affect the outputs.
  - [x] Wire it to the extension in `applyDefaults()`.
  - [x] In `exec()`, add the variable to `environment` only when the flag is
        `true`; when `false`, leave the inherited environment untouched so a
        variable exported for the daemon still reaches the fork.
- [x] Tests (`gradle-plugin/src/test`):
  - [x] `ExtensionSpec` — default `false` (a "can be set" case would only
        test Gradle's `Property`; dropped).
  - [x] `LaunchTaskEnvironmentSpec` — flag `true` adds the variable; flag
        `false` leaves a planted inherited value intact.
- [x] Docs: `gradle-plugin/README.md` configuration table + a short section on
      when to use it and its scope.
- [x] Version bump `.077` → `.079` (`.078` is taken by
      `origin/align-protobuf-runtime`), `./gradlew build`, dependency reports,
      `dokkaGenerate`.
- [x] Commits: `` Bump version -> `2.0.0-SNAPSHOT.079` ``, the feature, and
      `Update dependency reports`.

## Log

- 2026-10-19 — drafted in worktree `.claude/worktrees/disable-protobuf-version-check`
  branched from `origin/master` @ `977762d8f9`.
- 2026-10-19 — implemented; `./gradlew build dokkaGenerate` green; reviews by
  `kotlin-engineer` (APPROVE), `spine-code-review` and `review-docs`
  (APPROVE WITH CHANGES) applied: planted-value test, `protoc` plugins wording,
  `applyDefaults` KDoc exception note.
- 2026-10-19 — the worktree's pinned `.agents/shared` `version-bumped.sh` cannot
  parse `extra.set(...)` in `version.gradle.kts` (exit 2); the floated copy in
  the main checkout handles it. Fixed upstream; resolves on the next `config` pull.
