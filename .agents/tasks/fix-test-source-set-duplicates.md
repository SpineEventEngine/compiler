---
slug: fix-test-source-set-duplicates
branch: claude/vibrant-jang-fa76b9
owner: claude
status: in-review
started: 2026-06-30
---

## Goal

Fix [issue #19](https://github.com/SpineEventEngine/compiler/issues/19):
generated source sets in tests may contain duplicates. The Compiler reads the
`protoc` output and writes processed code into a separate directory; both must
not reach the Java/Kotlin compiler at once, or compilation fails with
`duplicate class` errors. This "sometimes does not work for the test source
set."

## Context

- `protoc` writes to `build/generated/sources/proto/<sourceSet>/{java,kotlin}`;
  the Compiler writes to `generated/<sourceSet>/{java,kotlin}`.
- Deduplication lived in `configureCompileTasks` (live compile-task filtering) in
  ProtoData until a 2023 commit ("Improve filtering of duplicated generated
  sources") replaced it with an eager, one-time source-set rewrite, now in
  tool-base's `GeneratedSourcePlugin.configureSourceSetDirs`.
- The eager rewrite is fragile: plugin-application order, a symlinked project
  path (the old `residesIn` compared a canonical path to a merely absolute one),
  or a consumer plugin re-adding the directory can leave the `protoc` output in
  the source set.

## Plan

- [x] Reproduce: `test-source-set` fixture with protos in `main` + `test`; an
      `afterEvaluate` re-adds the `protoc` output dir to the `test` source set,
      reproducing the leaked state. Confirmed `compileTestJava` fails with
      `duplicate class`.
- [x] Fix (compiler-side, order-independent): re-introduce live compile-task
      filtering in `Plugin.excludeProtocOutputFromCompilation()` — re-set
      `JavaCompile.source` to a filtered view and add an `exclude` spec to
      `KotlinCompile`. Skips the deprecated in-place mode.
- [x] Harden `Paths.residesIn` to canonicalize both operands (symlink-safe).
- [x] Regression test `keep duplicate generated classes out of the 'test'
      compilation` asserts `testClasses` succeeds.
- [x] Version bump `2.0.0-SNAPSHOT.057` → `.058`.
- [x] Full `PluginSpec` green: 16 tests, 0 failures (1 pre-existing skip).
      `:gradle-plugin:detekt` (the `check` gate) passes.

## Log
- 2026-06-30 — reproduced RED (duplicate class in `compileTestJava`), implemented
  the fix, verified GREEN with a marker proving the fixed plugin code executed in
  the inner testkit build.
- Note: local incremental/build-cache is stale in this checkout; verifying
  functional tests needs `--rerun-tasks --no-build-cache` and `arch -arm64`
  (gradle-doctor Rosetta check). See auto-memory `gradle-arch-arm64`.
