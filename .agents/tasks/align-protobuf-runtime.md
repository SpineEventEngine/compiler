---
slug: align-protobuf-runtime
branch: align-protobuf-runtime
owner: claude
status: in-progress
started: 2026-10-16
---

## Goal

The Protobuf runtime on the classpath of the forked Compiler JVM is the highest
of the versions that the CLI (`spineCompilerRawArtifact`) and the user classpath
(`spineCompiler`) ask for. It follows ordinary Gradle conflict resolution, so a
consumer whose Compiler plugins are built with a newer Protobuf no longer has to
force `Protobuf.libs`.

## Context

Since `2.0.0-SNAPSHOT.073` (`unbundle-protobuf`), `compiler-cli-all` declares
`protobuf-java`, `protobuf-java-util`, and `protobuf-kotlin` in its POM instead
of bundling them. `LaunchSpineCompiler` puts `spineCompilerRawArtifact` before
`spineCompiler` on the classpath, and the two configurations are resolved
separately. So the CLI's declared version shadows a newer one from the plugins,
whose gencode then fails with `ProtobufRuntimeVersionException`. Spine builds
are unaffected: `forceVersions()` forces `Protobuf.libs` in every configuration.

Options considered:

- **One graph for the whole classpath.** The order of the files within one
  resolved configuration is not under our control, and the fat JAR must stay
  first: the modules it bundles must keep winning. It would also drop rules that
  consumers attach to `spineCompilerRawArtifact` alone (`core-jvm`,
  `jdbc-storage` rewrite `compiler-cli-all` there).
- **`shouldResolveConsistentlyWith`.** Gradle locks every module of the source
  graph with `strictly` (`DefaultConfiguration.getConsistentResolutionVersionLocks`).
  One way round, it downgrades the CLI's runtime to an older user version; the
  other way, it downgrades a newer plugin runtime to the CLI's. A combined third
  configuration as the source would give the maximum, but its strict locks
  would fight configuration-specific rules like the ones above.
- **Derived constraints (chosen).** `spineCompilerRawArtifact` gets
  a `require` constraint for each `com.google.protobuf` module resolved in
  `spineCompiler`. Conflict resolution then picks the higher version. Other
  libraries are left alone: `spineCompiler` is an extension point that may hold
  any library a Compiler plugin brings (review of #90), so the constraints are
  scoped to the Protobuf runtime, the only part of it the CLI shares by design.
  A consumer's `force` on the configuration still wins; a lower `strictly`
  or lock fails the resolution (checked with a probe build).
  The constraints are added in `withDependencies`, right before resolution:
  `dependencyConstraints.addAllLater` would resolve, and so lock,
  `spineCompiler` whenever anything reads the constraints (`CliConfigurationSpec`).

## Plan

- [x] Bump the version to `2.0.0-SNAPSHOT.074` (above the wave's `.072` and
  `unbundle-protobuf`'s `.073`).
- [x] Functional test (`ProtobufRuntimeSpec`, fixture `protobuf-runtime-test`):
  a Compiler plugin subproject built with a given Protobuf, no forcing, and
  the gencode/runtime check turned back on in the forked JVM. Two cases:
  a newer plugin runtime wins; an older one does not downgrade the CLI's.
- [x] Derive the constraints in `Plugin.createConfigurations`.
- [x] Address the reviews of `/pre-pr`.
- [x] `build`, `/pre-pr`, PR (#90), Copilot review.
- [x] Scope the constraints to the Protobuf runtime (review of #90).

## Log

- 2026-10-16 — branch cut from `unbundle-protobuf` (`7217fed910`).
- 2026-10-16 — the newer case fails before the fix: "gencode 4.37.0-rc1,
  runtime 4.36.0". After it, both cases pass. The newer release is
  `4.37.0-RC1` because the wave moves the build to `4.36.2`, the latest stable.
