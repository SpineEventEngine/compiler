---
slug: transfer-assert-compilation-error
branch: claude/dazzling-rubin-g2qy8e
owner: claude
status: in-progress
started: 2026-06-18
---

## Goal

Make the `assertCompilationError` test assertion — introduced in
`core-jvm-compiler` PR #101 (`:base` test fixtures) — a first-class part of the
Spine Compiler `testlib` module, with tests. Then file an issue in
`core-jvm-compiler` to migrate to the transferred function once the Compiler is
released with it.

## Context

- Source: `core-jvm-compiler` `base/src/testFixtures/kotlin/io/spine/tools/core/jvm/Assertions.kt`.
- Target package in `testlib`: `io.spine.testing.compiler`.
- `testlib` already depends on `Logging.testLib` ("We need `tapConsole`.") and on
  `:api` (which provides `io.spine.tools.compiler.Compilation`).
- `AbstractCompilationErrorTest.assertCompilationFails` already duplicates the
  `assertThrows<Compilation.Error>` + `tapConsole` pattern — fold it onto the new
  shared function.

## Plan
- [ ] Add `Assertions.kt` with `public fun assertCompilationError(...)`.
- [ ] Refactor `AbstractCompilationErrorTest.assertCompilationFails` to reuse it.
- [ ] Add `AssertionsSpec.kt` covering: returns the error, captures console
      output, fails when no error is thrown.
- [ ] Bump version `2.0.0-SNAPSHOT.053` -> `.054` (separate commit).
- [ ] Verify compile, push, open draft PR.
- [ ] File migration issue in `core-jvm-compiler`.

## Log
- 2026-06-18 — drafted plan, executing.
