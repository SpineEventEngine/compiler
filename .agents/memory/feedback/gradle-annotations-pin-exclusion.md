---
name: gradle-annotations-pin-exclusion
description: Publish Gradle plugins without `org.jetbrains:annotations` — Gradle 9.6 pins it `strictly 13.0` on build script classpaths, and consumer-side `force(...)` is a band-aid that spreads.
metadata:
  type: feedback
  since: 2026-07-24
---

Gradle 9.6 constrains `org.jetbrains:annotations` to `{strictly 13.0}`
("Pinned to the embedded Kotlin") on build script classpaths carrying
Kotlin-ecosystem plugins, while `kotlinx-coroutines` (via Aedile, gRPC
Kotlin stubs, and the Kotlin Gradle plugin itself) requires `23.0.0`.
Whether Gradle reconciles the two is **graph-shape sensitive**: an
unrelated dependency bump (CoreJvm `.510 → .521`) flipped resolution
from a clean `23.0.0 → 13.0` downgrade to a hard
`Cannot find a version of 'org.jetbrains:annotations'` failure.

**Why:** We repeatedly patched the symptom with
`resolutionStrategy { force(JetBrainsAnnotations.lib) }` — root build,
`tests/*`, twelve fixture templates — and every consumer of the plugin
would have needed the same workaround. The producer-side fix removes the
conflicting requirement from the published metadata once, for everyone:
the annotations are compile-time metadata, and consumers still get
`13.0` through the `kotlin-stdlib` edge, which satisfies the pin.

**How to apply:** In every published Gradle plugin artifact, exclude
`org.jetbrains:annotations` from all published dependencies (see
`excludeJetBrainsAnnotations()` in `gradle-plugin/build.gradle.kts` and
the Aedile exclusion in `api/build.gradle.kts`). Do not add new
consumer-side `force(...)` workarounds for this module. To verify a fix
or reproduce the failure, build a consumer-mirror probe: a scratch
project applying `kotlin("jvm")` + the plugin from `mavenLocal()` with
no forces — it fails or passes with the published metadata alone. The
CoreJvm Compiler plugin still needs the same exclusion in its repo;
until it ships, `tests/compiler-extension` keeps its force (comment in
that file states the removal condition).

Related: [[functional-test-fixtures]]
