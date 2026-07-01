---
name: integration-tests-stale-compiler-after-version-bump
description: After a version bump, an incremental `integrationTest` can silently launch the OLD compiler — clean-build it.
metadata:
  type: project
  since: 2026-06-30
---

After bumping `compilerVersion` in `version.gradle.kts`, an **incremental**
`./gradlew integrationTest` can silently exercise the **previous** compiler
version, masking or falsely reproducing bugs.

The Compiler Gradle plugin records its own version in a generated resource
(`version.txt` / `META-INF/io.spine/…compiler-gradle-plugin.meta`), and
`io.spine.tools.compiler.gradle.plugin.Plugin.version` reads it to decide which
`compiler-cli-all:<version>` the consumer launches (`LaunchSpineCompiler`). That
resource is **not reliably regenerated** on an incremental build when only the
version changes, so a freshly published `…-N` plugin can still embed `…-(N-1)`
and launch the older CLI from Maven Local — even though the `…-N` `compiler-api`
/ `compiler-cli-all` jars themselves contain the new bytecode.

**Why:** the version-bump step changes `version.gradle.kts` but the metadata
task's up-to-date check (and the build cache) may not treat that as an input, so
the stale resource survives. CI never hits this because it builds clean.

**How to apply:** after a version bump, verify integration with a clean build —
`./gradlew clean integrationTest --no-build-cache` — not an incremental one. To
diagnose which compiler a consumer actually launches, run
`cd tests && ./gradlew :consumer:dependencies --configuration spineCompilerRawArtifact`
and confirm the resolved `compiler-cli-all` version matches `version.gradle.kts`.
To confirm a published jar has the expected code, `javap -l -p` the bundled
`io/spine/tools/compiler/render/*.class` and compare line numbers. The compiler
runs as the `compiler-cli-all` fat jar via the CLI, not the plain api jar.
