---
name: functional-test-fixtures
description: Authoring `gradle-plugin` functionalTest fixture projects — copied buildSrc,
  import rules, Maven Local prerequisite, runner tuning.
metadata:
  type: project
  since: 2026-07-06
---

Fixture projects under `gradle-plugin/src/functionalTest/resources/<name>/` are TestKit
builds materialized via `GradleProject.setupAt(dir).fromResources("<name>")` — see
`PluginSpec` and `LaunchTaskCoverageSpec` for the setup chain.

**Why:** The fixtures compile against a copied `buildSrc` and resolve the plugin from
Maven Local, so failures surface only at fixture-build time inside a running test —
a slow feedback loop when the wiring is wrong (one missed import costs a full
`functionalTest` cycle).

**How to apply:**

- `copyBuildSrc()` copies the repo's `buildSrc` into the fixture, so fixture build
  scripts may use `io.spine.dependency.*` objects (e.g. `Jacoco.version`) directly.
- Top-level buildSrc helpers (`standardSpineSdkRepositories()`) resolve without an
  import, but `standardToSpineSdk()` needs
  `import io.spine.gradle.repo.standardToSpineSdk` — copy the imports of an existing
  fixture (`launch-test`) when creating a new one.
- The `functionalTest` task depends on `publishToMavenLocal` of every production
  module; fixtures resolve `id("io.spine.compiler") version "@COMPILER_VERSION@"`
  (replaced with `Plugin.version`) from `mavenLocal()`.
- After `builder.create()`, call `project.tuneRunner()` (shared helper in the
  `functionalTest` source set) instead of tuning the TestKit runner inline.
