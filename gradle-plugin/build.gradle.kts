/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase
import io.spine.dependency.test.JUnit
import io.spine.gradle.isSnapshot

plugins {
    module
    id("io.spine.artifact-meta")
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish").version("1.3.1")
    `version-to-resources`
    `write-manifest`
}

artifactMeta {
    // Add `protoc` as an explicit dependency as we pass it on to
    // `protobuf/protoc/artifact` when configuring a project.
    addDependencies(Protobuf.compiler)
    excludeConfigurations {
        containing(
            "errorprone",
            "detekt",
            "jacoco",
            "pmd",
            "checkstyle",
            "ksp",
            "dokka",
            "jvm-tools",
        )
    }
}

@Suppress(
    "UnstableApiUsage" /* testing suites feature */,
    "unused" /* suite variable names obtained via `by` calls. */
)
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(JUnit.version)
            dependencies {
                implementation(Kotlin.GradlePlugin.lib)
                implementation(gradleKotlinDsl())
                implementation(Protobuf.GradlePlugin.lib)
                implementation(ToolBase.pluginBase)
                implementation(ToolBase.pluginTestlib)
            }
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(JUnit.version)
            dependencies {
                implementation(Kotlin.GradlePlugin.lib)
                implementation(Kotlin.testJUnit5)
                implementation(ToolBase.pluginBase)
                implementation(TestLib.lib)
                implementation(ToolBase.pluginTestlib)
                implementation(project(":gradle-plugin"))
            }
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly(Protobuf.GradlePlugin.lib)
    compileOnly(Kotlin.GradlePlugin.api)

    api(project(":gradle-api"))
    api(ToolBase.gradlePluginApi)

    implementation(project(":api"))
    implementation(project(":params"))
    implementation(ToolBase.lib)
    implementation(ToolBase.jvmTools)
    implementation(ToolBase.pluginBase)
}

/**
 * Make functional tests depend on publishing all the submodules to Maven Local so that
 * the Gradle plugin can get all the dependencies when it's applied to the test projects.
 */
@Suppress("unused")
val functionalTest: Task by tasks.getting {
    val task = this
    productionModules.forEach { subproject ->
        task.dependsOn(":${subproject.name}:publishToMavenLocal")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

val compilerVersion: String by extra
val isSnapshot = compilerVersion.isSnapshot()

val publishPlugins: Task by tasks.getting {
    enabled = !isSnapshot
}

@Suppress("unused")
val publish: Task by tasks.getting {
    if (!isSnapshot) {
        dependsOn(publishPlugins)
    }
}

publishing {
    repositories {
        mavenLocal()
        if (isSnapshot) {
            remove(gradlePluginPortal())
        }
    }
    publications.withType<MavenPublication>().all {
        groupId = "io.spine.tools"
        artifactId = "compiler-gradle-plugin"
        version = compilerVersion
    }
}

gradlePlugin {
    website.set("https://spine.io/")
    vcsUrl.set("https://github.com/SpineEventEngine/compiler.git")
    plugins {
        create("spineCompilerGradlePlugin") {
            id = "io.spine.compiler"
            implementationClass = "io.spine.tools.compiler.gradle.plugin.Plugin"
            displayName = "Spine Compiler Gradle Plugin"
            description =
                "Sets up the Spine Compiler to be used in your project."
            tags.set(listOf("spine", "ddd", "protobuf", "compiler", "code-generation", "codegen"))
        }
    }
    val functionalTest by sourceSets.getting
    testSourceSets(
        functionalTest
    )
}


tasks {
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }

    publishPlugins {
        notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/21283")
    }
}

/**
 * Configure task dependencies here because the call in `subprojects` does not
 * have the effect on the dependency of the `publishPluginJar` on `createVersionFile`.
 *
 * We do we on `afterEvaluate` to avoid earlier than needed task creation
 * when `findByName()` is called.
 */
afterEvaluate {
    configureTaskDependencies()
}
