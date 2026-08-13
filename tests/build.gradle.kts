/*
 * Copyright 2026, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import io.spine.dependency.boms.BomsPlugin
import io.spine.dependency.lib.Caffeine
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.JacksonV2
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.repo.standardToSpineSdk
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * The references below must stay fully qualified: Gradle compiles the extracted
 * `buildscript` block separately from the rest of the script, without the
 * file-level `import` statements. Short names fail here with
 * "Unresolved reference" at script compilation time.
 */
@Suppress("RemoveRedundantQualifierName")
buildscript {
    dependencies {
        classpath(io.spine.dependency.lib.Protobuf.GradlePlugin.lib)
        classpath(io.spine.dependency.lib.Kotlin.GradlePlugin.lib)
    }
}

plugins {
    java
    `kotlin-dsl`
    kotlin("jvm") apply false
    id("com.google.protobuf")
    idea
}
apply<BomsPlugin>()

repositories.standardToSpineSdk()

subprojects {
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("idea")
        plugin("com.google.protobuf")
        plugin("module-testing")
        from("$rootDir/../version.gradle.kts")
    }
    apply<BomsPlugin>()

    val compilerVersion = extra["compilerVersion"] as String
    group = "io.spine.compiler.tests"
    version = compilerVersion

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    repositories.standardToSpineSdk()

    configurations {
        forceVersions()
        all {
            exclude(group = "io.spine", module = "spine-flogger-api")
            exclude(group = "io.spine", module = "spine-logging-backend")

            resolutionStrategy {
                Grpc.forceArtifacts(project, this@all, this@resolutionStrategy)
                Jackson.forceArtifacts(project, this@all, this@resolutionStrategy)
                Jackson.DataType.forceArtifacts(project, this@all, this@resolutionStrategy)
                Jackson.DataFormat.forceArtifacts(project, this@all, this@resolutionStrategy)

                /*
                   Jackson 2.x reaches this build only transitively — Palantir Java
                   Format and the IntelliJ Platform both consume it — while `Jackson`
                   declares the 3.x line under the `tools.jackson` group and so no
                   longer aligns `com.fasterxml.jackson.*`. Whenever those consumers
                   disagree on a version, the `failOnVersionConflict()` set by
                   `forceVersions()` above turns the divergence into a build failure
                   instead of resolving it.

                   A BOM cannot do this job here: the Protobuf plugin configurations
                   such as `compileProtoPath` ignore `enforcedPlatform`. Matching the
                   whole group also covers the artifacts `JacksonV2` does not model,
                   e.g. `jackson-jr-objects` and `jackson-module-kotlin`.

                   `jackson-annotations` is excluded on purpose: Jackson 3.x keeps
                   consuming it at its own version, forced via `Jackson.annotations`.
                 */
                eachDependency {
                    val jackson2 = requested.group.startsWith(JacksonV2.group)
                    if (jackson2 && requested.name != "jackson-annotations") {
                        useVersion(JacksonV2.version)
                        because("Align the Jackson 2.x line pulled in transitively.")
                    }
                }

                @Suppress("DEPRECATION") // To force `Kotlin.stdLibJdk7`.
                force(
                    Grpc.bom,
                    Jackson.bom,
                    Jackson.annotations,
                    Kotlin.bom,
                    KotlinPoet.lib,
                    Caffeine.lib,
                    Base.annotations,
                    Base.lib,
                    Base.environment,
                    Base.format,
                    Time.lib,
                    Time.javaExtensions,
                    ToolBase.lib,
                    ToolBase.psiJava,
                    ToolBase.jvmTools,
                    ToolBase.pluginBase,
                    ToolBase.gradlePluginApi,
                    ToolBase.protobufSetupPlugins,
                    Compiler.api(compilerVersion),
                    Validation.runtime,
                    Validation.javaBundle,
                    Logging.lib,
                    Logging.grpcContext,
                    Logging.libJvm,
                    Reflect.lib,
                    CoreJvm.server,
                    "io.spine.validation:spine-validation-java-runtime:2.0.0-SNAPSHOT.352"
                )
            }
        }
    }

    disableDocumentationTasks()

    kotlin {
        compilerOptions {
            jvmTarget.set(BuildSettings.jvmTarget)
            setFreeCompilerArgs()
        }
    }

    val generatedFiles = "$projectDir/generated"
    tasks.getByName<Delete>("clean") {
        delete.add(generatedFiles)
    }

    tasks.withType<JavaExec>().configureEach {
        if (name.contains("SpineCompiler")) {
            systemProperty("jdk.attach.allowAttachSelf", "true")
        }
    }

    dependencies {
        Protobuf.libs.forEach { implementation(it) }
    }
}
