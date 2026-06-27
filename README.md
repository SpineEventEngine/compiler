# Spine Compiler

[![Build on Ubuntu][ubuntu-badge]][ubuntu-build]
[![Build on Windows][windows-badge]][windows-build]
[![codecov.io][codecov-badge]][codecov-report]
[![license][apache-badge]][apache-license]

Spine Compiler is a collection of tools for generating quality domain models from Protobuf
definitions. It plugs into your Gradle build and extends the code that `protoc` produces,
turning plain `.proto` messages into rich domain types — with validation, factory methods,
and other conveniences contributed by pluggable code generators.

The Compiler is part of the [Spine SDK][spine-site] and powers code generation across
Spine-based projects. It is not limited to them, though: any Protobuf-based JVM project can
apply the Compiler and drive it with its own code-generation plugins.

## How it works

The Compiler treats your Protobuf definitions as a model and processes it reactively:

1. `protoc` parses the `.proto` files. A `protoc` plugin bundled with the Compiler captures
   the result as a code-generation request.
2. The Compiler reads that request and represents the Protobuf files, types, fields, and
   options as a model, delivered as a stream of events.
3. The plugins you attach observe the model through *views* and react to it through
   *policies*, then emit source code through *renderers*.
4. The renderers create new source files or edit the ones `protoc` generated, producing the
   final domain model.

A *plugin* bundles these parts together, so attaching one plugin is enough to contribute a
whole feature to the generated code.

## Getting started

### Requirements

- JDK 17 or newer.
- The [Protobuf Gradle Plugin][protobuf-gradle] (`com.google.protobuf`), which the Compiler
  builds upon.

### Apply the Gradle plugin

The Compiler ships as a Gradle plugin published to the Gradle Plugin Portal under the
`io.spine.compiler` ID. Apply it alongside the Protobuf Gradle Plugin:

```kotlin
plugins {
    java
    id("com.google.protobuf")
    id("io.spine.compiler") version "2.0.0"
}
```

The example applies the `java` plugin; use `kotlin("jvm")` instead for a Kotlin project — the
Compiler generates code for both. Once applied, the Compiler hooks into the `generateProto`
tasks, so the generated model is refreshed on every build.

### Configure code-generation plugins

Out of the box the Compiler runs the pipeline without changing the generated code. To make it
do something, attach one or more Compiler plugins: list their classes in the `compiler { }`
block — nested inside the `spine { }` extension — and put them on the Compiler classpath with
the `spineCompiler` configuration.

```kotlin
spine {
    compiler {
        plugins(
            "com.acme.codegen.MyPlugin",
        )
    }
}

dependencies {
    spineCompiler("com.acme:my-codegen:1.0.0")
}
```

Then build the project as usual:

```bash
./gradlew build
```

## Writing a plugin

A Compiler plugin is a class with a parameterless constructor that bundles the components doing
the work. The most common component is a *renderer*, which creates or edits source files:

```kotlin
import io.spine.tools.code.Java
import io.spine.tools.compiler.plugin.Plugin
import io.spine.tools.compiler.render.Renderer
import io.spine.tools.compiler.render.SourceFileSet

/** Modifies the Java sources produced for the Protobuf model. */
public class MyRenderer : Renderer<Java>(Java) {

    override fun render(sources: SourceFileSet) {
        // Inspect the Protobuf model and create or edit source files here.
    }
}

/** Exposes the renderer — and any views or policies — to the Compiler. */
public class MyPlugin : Plugin(renderers = listOf(MyRenderer()))
```

Compile the plugin against the public API artifact:

```kotlin
dependencies {
    implementation("io.spine.tools:compiler-api:2.0.0")
}
```

Renderers can inspect the Protobuf model through views and react to it through policies. See the
`api` module and its API documentation for the full set of building blocks.

## Modules

The build publishes its modules under the `io.spine.tools` group with the `compiler-` prefix
(for example, `io.spine.tools:compiler-api`). The Gradle plugin is published separately to the
Gradle Plugin Portal as `io.spine.compiler`.

| Module          | Role                                                              |
|-----------------|------------------------------------------------------------------|
| `gradle-plugin` | The `io.spine.compiler` Gradle plugin — the entry point for most users. |
| `api`           | Public API for writing plugins, renderers, views, and policies.  |
| `gradle-api`    | Public Gradle-facing API: settings, task names, and artifact names. |
| `backend`       | The code-generation engine that runs the pipeline.               |
| `protoc-plugin` | The `protoc` plugin that captures the code-generation request.   |
| `cli`           | Command-line entry point that runs the pipeline.                 |
| `params`        | The parameter model passed to the Compiler.                      |
| `jvm`           | JVM-specific code-generation support.                            |

## Further reading

- [Spine SDK website][spine-site]
- [Contributing guidelines](CONTRIBUTING.md)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

Spine Compiler is released under the [Apache License, Version 2.0](LICENSE.md).

[ubuntu-badge]: https://github.com/SpineEventEngine/compiler/actions/workflows/build-on-ubuntu.yml/badge.svg
[ubuntu-build]: https://github.com/SpineEventEngine/compiler/actions/workflows/build-on-ubuntu.yml

[windows-badge]: https://github.com/SpineEventEngine/compiler/actions/workflows/build-on-windows.yml/badge.svg
[windows-build]: https://github.com/SpineEventEngine/compiler/actions/workflows/build-on-windows.yml

[codecov-badge]: https://codecov.io/github/SpineEventEngine/compiler/coverage.svg?branch=master
[codecov-report]: https://codecov.io/github/SpineEventEngine/compiler?branch=master

[apache-badge]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat
[apache-license]: https://www.apache.org/licenses/LICENSE-2.0

[spine-site]: https://spine.io/
[protobuf-gradle]: https://github.com/google/protobuf-gradle-plugin
