# ProtoData Gradle plugin

This Gradle plugin allows Java developers launch ProtoData without extra CLI commands.

## Usage

### Applying the plugin

To apply the plugin to the project, use the `plugins { }` block syntax.

```kotlin
plugins {
    id("io.spine.compiler") version("<ProtoData version>")
}
```

Or, alternatively, use the old-fashioned `buildscript { }` syntax.

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    
    dependencies {
        classpath("io.spine:compiler:<ProtoData version>")
    }
}

apply(plugin = "io.spine.compiler")
```

See the plugin [homepage](https://plugins.gradle.org/plugin/io.spine.protodata) for more.

### Launching ProtoData

When used from Gradle, ProtoData does not require installation.

Just launch [configure](#Configuration) code generation and start a build:
```
./gradlew build
```

### Configuration

ProtoData requires Renderers and Plugins for meaningful operation. You can specify those and more
via a Gradle extension.

Here is the complete list of configuration options:

| Name                      | Format              | Description                                                                                                                                   | Default value                      |
|---------------------------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------|
| `renderers`               | Java class names    | Implementations of `io.spine.compiler.renderer.Renderer`. Renderer ordering is preserved.                                                    | N/A                                |
| `plugins`                 | Java class names    | Implementations of `io.spine.compiler.plugin.Plugin`.                                                                                        | N/A                                |
| `optionProviders`         | Java class names    | Implementations of `io.spine.compiler.option.OptionProvider`.                                                                                | N/A                                |                                
| `requestFile`             | File path           | Where the serialized `CodeGeneratorRequest` should be stored.                                                                                 | A file under the `build` dir.      |      
| `srcBaseDir`              | Directory path      | Base directory where files generated by Protoc are located.                                                                                   | A directory under the `build` dir. | 
| `targetBaseDir`           | Directory path      | Base directory where files generated by ProtoData are placed. Files from the source directory are copied into the target directory as well.   | `$projectDir/generated/`           |           
| `subDir`                  | Directory path part | Subdirectory within `srcBaseDir` where files generated by the given Protoc plugin are located. This structure is mirrored in `targetBaseDir`. | `java`                             |                             
| Configuration `protoData` | Dependencies        | The dependencies required to launch ProtoData with the given args.                                                                            | N/A                                |                                

A complete configuration may look as follows:
```kotlin
plugins {
    id("io.spine.compiler") version("<ProtoData version>")
}

protoData {
    renderers("com.acme.MyInsertionPointPrinter", "com.acme.MyRenderer", "org.example.Renderer")
    plugins("com.acme.MyPlugin")
    optionProviders("com.acme.MyOptions")

    requestFile("${rootProject.buildDir}/commonRequestFile/request.bin")
    srcBaseDir("$projectDir/my-generated-files/")
    targetBaseDir("$projectDir/my-complete-files/")
    subDir("foobar")
}

dependencies {
    protoData(project(":my-compiler-plugin"))
    protoData("org.example:compiler-plugin:1.42")
}
```

## Source sets

For each source set in the project, the plugin generates a distinct task launching ProtoData.
Each task only processes the sources of the associated source set. This allows users to apply
ProtoData to production and test code alike.

To reference a specific task by name, use the following format: `launch<source set name>ProtoData`,
for example, `launchProtoData`, `launchTestProtoData`, `launchIntegrationTestProtoData`, etc.

To find all the tasks in a Gradle script, use the `LaunchProtoData` task type. For example:
```kotlin
tasks.withType<LaunchProtoData> {
    onlyIf { moon.phase >= 0.7 }
}
```

## Generated files

The plugin changes the configuration of the Protobuf Gradle plugin in such a way that the files
generated from Protobuf are placed under the `build` directory. After the files are then processed
by ProtoData, they will end up in ProtoData target directory, which is, by default,
`$projectDir/generated/`. To change the output dir, see the ProtoData plugin configuration.

## Caveat

The plugin relies on the Java Project structure, the Java Gradle plugin, the Protobuf Gradle
plugin, and the ProtoData Maven repository.

To make everything work, at this stage, users have to add the following config:

```kotlin
plugins {
    java
    id("com.google.protobuf") version("<Protobuf plugin version>")
    id("io.spine.compiler") version("<ProtoData version>")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/SpineEventEngine/ProtoData")
        credentials {
            username = "<GitHub Actor>"
            password = "<GitHub Token/Personal Access Token>"
        }
    }
}
```

Users who wish to extend ProtoData must also add the dependency to the API:
```kotlin
dependencies {
    implementation("io.spine.compiler:compiler-compiler:<ProtoData version>")
}
```
