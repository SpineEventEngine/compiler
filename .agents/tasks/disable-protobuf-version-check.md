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

The property controls `TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK` in the environment
of every `LaunchSpineCompiler` fork: `true` sets it to `true` (check off), `false`
removes it (check on, even if the build exports it), not set (default) inherits it
from the build. A default that cleared the variable would silently break builds
relying on the exported variable, including this repo's functional tests
(`RunnerTuning.tuneRunner()`), hence three states rather than `false` by default.

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
- End-to-end: the `protobuf-runtime-test` fixture from #90 removes the variable in
  the fork. `DisableProtobufVersionCheckSpec` reuses it, forcing the CLI runtime
  below the plugin gencode (`4.37.0-RC1`) so the check really fails.

## Plan

- [x] `CompilerSettings` (`gradle-api`): add
      `public val disableProtobufVersionCheck: Property<Boolean>` with KDoc.
- [x] `Extension` (`gradle-plugin`): implement it, not set by default.
- [x] `LaunchSpineCompiler`:
  - [x] `@get:Internal internal abstract val disableProtobufVersionCheck:
        Property<Boolean>` — internal because it does not affect the outputs.
  - [x] Wire it to the extension in `applyDefaults()`.
  - [x] In `exec()`, `applyProtobufVersionCheckSetting()`: `true` sets the
        variable, `false` removes it, not set leaves the environment intact.
- [x] Tests (`gradle-plugin/src/test`):
  - [x] `ExtensionSpec` — not set by default.
  - [x] `LaunchTaskEnvironmentSpec` — not set keeps a planted inherited value;
        `true` sets the variable; `false` removes a planted `true`.
- [x] Functional test `DisableProtobufVersionCheckSpec` — `false` keeps the check
      even when the build sets the variable (fails with
      `ProtobufRuntimeVersionException`); `true` lets the Compiler run.
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
- 2026-10-19 — PR #95 opened; merged `origin/master` after #90 landed (conflicts only
  in `version.gradle.kts` and reports; kept `.079`); CodeMatters headers applied.
- 2026-10-19 — per review by the user, `false` now removes the variable; the default
  became "not set" to keep the exported-variable workaround working. Added the
  end-to-end spec.
