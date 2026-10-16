---
slug: unbundle-protobuf-runtime
branch: unbundle-protobuf
owner: claude
status: in-review
started: 2026-10-16
---

## Goal

The Compiler CLI fat JAR (`compiler-cli-all`, `cli/build.gradle.kts`) stops
bundling the Protobuf runtime. It declares it as a `runtime` dependency instead,
so that consumers choose the Protobuf runtime of the Compiler JVM by
ordinary dependency resolution.

## Context

`LaunchSpineCompiler` runs the CLI with the `spineCompilerRawArtifact`
configuration (this JAR) on the classpath *before* the user classpath
(`spineCompiler`, the Compiler plugins of the project and their dependencies).
The JAR embedded unrelocated `protobuf-java`, `protobuf-java-util`, and
`protobuf-kotlin` classes — `RuntimeVersion` read `4.36.0` in `.071`. So the
bundled runtime always won. In the dependency refresh wave
`cascade-refresh-a0cbbbf`, the Validation renderers built against Protobuf
`4.36.2` failed to load with `ProtobufRuntimeVersionException`, and `core-jvm`
and `jdbc-storage` had to lift `compiler-cli-all` to a build made with `4.36.2`.

Relocation is not an option: the Protobuf runtime depends on the names of its
classes, and the plugins exchange Protobuf messages with the Compiler.

With the runtime declared in the POM, `spineCompilerRawArtifact` resolves it, and
the `force(Protobuf.libs)` that Spine builds apply to every configuration
(`forceVersions()`) selects the consumer's version.

`compiler-protoc-plugin` (`-exe.jar`) still bundles the runtime, and it should:
`protoc` launches it in a JVM of its own, where nothing else shares its classpath.

Limits: `spineCompilerRawArtifact` and the user classpath are still resolved
separately, and the former comes first. A consumer that does not force
`Protobuf.libs` gets the version this JAR declares, as before, but can now
override it. Resolving both from one graph in the Gradle plugin would remove
the need to force; that is a separate change. The configuration also now needs
a repository hosting Protobuf, which every project compiling Protobuf has.

## Plan

- [x] Exclude `Protobuf.libs` from `shadowJar` by the dependency filter.
- [x] Declare them, non-transitively, in the `cliFatJar` POM and in
  a `protobufRuntime` configuration extended by `shadowRuntimeElements`
  (not `shadow`, which Shadow writes into the manifest `Class-Path`).
- [x] Replace the inline code with `unbundleProtobufRuntime(publication = "cliFatJar")`
  of config (config#796), taken with `./config/pull`.
- [x] Verify: no Protobuf runtime classes in the JAR; the POM declares
  `Protobuf.libs`; the functional tests run the CLI from Maven Local.
- [x] `build`, `/pre-pr`.
- [ ] PR.

## Log

- 2026-10-16 — implemented; version `2.0.0-SNAPSHOT.073` (skips `.072`, which
  the wave needs for its re-bump above `.071`).
- 2026-10-16 — `build dokkaGenerate --continue`: green except one functional test,
  `PluginSpec` > copy 'grpc', which needs the x86_64 `protoc-gen-grpc-java` 1.50.2
  on an arm64 host without Rosetta (environmental; CI on Linux is unaffected).
  The other 16 functional tests launch the CLI `.073` from Maven Local and pass.
- 2026-10-16 — merged master (#91, `.072`), pulled config, switched to config's function.
