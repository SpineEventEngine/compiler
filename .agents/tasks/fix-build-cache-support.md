---
slug: fix-build-cache-support
branch: claude/sleepy-mendel-gn89dd
owner: claude
status: in-review
started: 2026-06-09
---

## Goal

Builds of projects using the Compiler Gradle plugin succeed with
`org.gradle.caching=true`: after `./gradlew clean build`, the code generated
by `LaunchSpineCompiler` is present even when `generateProto` and/or
`LaunchSpineCompiler` are restored from the Gradle build cache.

## Context

With the build cache on, `clean build` fails because no code is generated.

Root cause: the Compiler `protoc` plugin writes the `CodeGeneratorRequest`
file (`build/spine/compiler/requests/<sourceSet>.bin` plus a `.pb.json` twin)
as a *side effect* of `GenerateProtoTask`, but the file is not declared as an
output of that task. `GenerateProtoTask` is `@CacheableTask`; after `clean`
its declared outputs are restored from the cache *without executing protoc*,
so the request file is never recreated. `LaunchSpineCompiler` is configured
with `onlyIf { hasRequestFile(...) }`, so it is SKIPPED (its own cache entry
is not even consulted), and compilation then fails for the missing generated
code. With `org.gradle.caching=false`, `generateProto` always re-executes
after `clean`, recreating the request file — hence "everything works".

A related gap: `LaunchSpineCompiler` *reads* the request file and the
settings directory but declares neither as inputs, so its up-to-date checks
and cache key do not reflect them.

## Plan

- [x] Diagnose the root cause (confirmed against `GenerateProtoTask` 0.10.0
      sources: `@CacheableTask`, outputs = `outputBaseDir` + descriptor set
      only; plugin options are inputs).
- [x] `Plugin.kt`: declare the request file and its `.pb.json` twin as
      outputs of `GenerateProtoTask` so the build cache stores/restores them.
- [x] `LaunchSpineCompiler.kt`: declare the request file and the settings
      directory as inputs.
- [x] Add a functional test: `build` → `clean` → `build` with
      `--build-cache` and a project-local cache directory; assert
      `generateProto` is `FROM_CACHE`, the launch task is restored (not
      SKIPPED), and `generated/main/{java,kotlin}` exist.
- [ ] Verify via CI (local verification impossible: the sandbox blocks
      Spine artifact repositories; see Log).

## Log

- 2026-06-09 — Investigated; confirmed root cause from protobuf-gradle-plugin
  0.10.0, tool-base (`rootWorkingDir` = `build/spine`), and base
  (`CodeGeneratorRequestWriter` writes `<ss>.bin` and `<ss>.pb.json`).
- 2026-06-09 — The remote sandbox cannot resolve `io.spine.*` artifacts
  (403 from all Spine repos), so `./gradlew build` cannot run here.
  Verification delegated to CI on the PR.
- 2026-06-09 — Implemented; version bumped to `2.0.0-SNAPSHOT.051`.
  Dependency reports (`dependencies.md`, `pom.xml`) not regenerated for
  the same reason; regenerate with the first local/CI build.
- 2026-06-09 — CI: the functional test failed because `build-cache-test`
  (copied from `launch-test`) lacked the Protobuf `implementation`
  dependencies needed to compile generated code; now mirrors
  `java-kotlin-test`. The `:integrationTest` failure
  (`tests/build.gradle.kts:49`, unresolved `Protobuf`/`Kotlin` in
  `buildscript`) is pre-existing: the base tip `ab708f79` fails
  identically on `update-dependencies`.
- 2026-06-09 — Per review: `--build-cache` enabled for the
  `integrationTest` child build (consumes the locally-published fixed
  plugin). The global `org.gradle.caching` stays off until the
  `io.spine.core-jvm` plugin used by this repo's own build consumes
  a Compiler with this fix.
- 2026-06-10 — The functional test still fails on CI with the *same*
  17 `compileKotlin` errors as before the Protobuf deps were added,
  while `java-kotlin-test` (byte-identical build script) passes in the
  same run. The settings file (with the cache block) *was* in effect.
  Suspecting stale/shadowed test resources keyed by the directory name;
  renamed `build-cache-test` → `cached-build-test` and added a canary
  assertion that the copied build script declares `Protobuf.libs`.
  If the canary passes and compilation still fails, the problem is in
  the `--build-cache` + `compileKotlin` interplay, not in resources.
