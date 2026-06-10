---
slug: fix-tests-build-script-classpath
branch: claude/adoring-tesla-1cff28
owner: claude
status: in-progress
started: 2026-06-09
---

## Goal

PR #66 CI is green again on both Ubuntu and Windows: the nested `tests`
build compiles its `build.gradle.kts` instead of failing with
`Unresolved reference 'Protobuf'` / `'Kotlin'` (Ubuntu + Windows) or
`Unresolved reference 'io'` (Windows before commit `e8fa227`).

## Context

- The Kotlin DSL extracts the `buildscript {}` block and compiles it as a
  separate "stage 1" program **without the file-level `import` statements**.
  Reproduced in isolation with Gradle 9.5.1: short names fail, fully
  qualified names compile. All other `buildscript` blocks in the repo
  already use fully qualified names for this reason.
- Commit `e8fa227` (copilot-swe-agent) replaced the fully qualified names in
  `tests/build.gradle.kts` with imported short names, breaking script
  compilation on **all** platforms.
- The original Windows-only failure (`Unresolved reference 'io'`, run
  27230621848) has a different cause: the updated Windows workflow sets
  `git config --global core.symlinks false`, so the `tests/buildSrc ->
  ../buildSrc` symlink is checked out as a plain text file. The nested
  build launched by `integrationTest` then has no `buildSrc` at all, and
  no `io.spine.dependency.*` classes on the script classpath.
  (`tests/gradle.properties` degrades the same way.)

## Plan

- [x] Diagnose Ubuntu failure (stage-1 imports) and Windows failure
      (symlink checked out as text file).
- [x] Revert `e8fa227`: restore fully qualified names in the
      `buildscript {}` block of `tests/build.gradle.kts`; document why
      the qualifiers must stay.
- [x] In root `build.gradle.kts`, make `integrationTest` materialize
      `tests/buildSrc` and `tests/gradle.properties` as real copies when
      the symlinks were checked out as placeholder text files.
- [x] Re-enable Git symlinks on Windows CI (`core.symlinks true`):
      the symlinked `tests/buildSrc` layout was green on Windows for
      ~3 years (symlink since 2023-02; `.agents` links since 2025-09;
      last green Windows run 2026-03-12) until this branch's `config`
      update introduced `core.symlinks false`. The materialization in
      root `build.gradle.kts` stays as a no-op fallback for
      symlink-less checkouts (and for future `config` syncs that may
      reintroduce the setting; the upstream fix belongs in `config`).
- [x] Sanity-check what is verifiable locally; commit, push, draft PR
      into `update-dependencies`.
- [ ] Confirm both CI jobs are green on PR #66 after the fix lands.

## Log

- 2026-06-09 — diagnosed both failures; synthetic repro of the stage-1
  import limitation under Gradle 9.5.1 (`/tmp/k1`): short name fails,
  FQN passes.
- 2026-06-09 — `materializeTestsLink` verified in the synthetic project:
  placeholder files are replaced by real copies (`build/`, `.gradle/`
  excluded); healthy symlinks and already-materialized dirs are left
  untouched. Full local build not possible: the sandbox network policy
  blocks Spine repositories (403).
