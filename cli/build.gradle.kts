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

import io.spine.dependency.lib.AutoService
import io.spine.dependency.lib.AutoServiceKsp
import io.spine.dependency.lib.Clikt
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.gradle.publish.SpinePublishing
import io.spine.gradle.publish.setup

plugins {
    module
    application
    `write-manifest`
    `build-proto-model`
    `maven-publish`
    prototap
    id("com.gradleup.shadow")
}

dependencies {
    listOf(
        Clikt.lib,
        ToolBase.jvmTools,
        Logging.lib,
        Logging.libJvm,
    ).forEach { implementation(it) {
        exclude(group = "io.spine.validation", module = "spine-validation-java-runtime")
    } }

    implementation(kotlin("reflect"))

    listOf(
        ":api",
        ":params",
        ":backend",
        ":jvm",
    ).forEach { implementation(project(it)) {
        exclude(group = "io.spine.validation", module = "spine-validation-java-runtime")
    } }

    testAnnotationProcessor(AutoService.processor)?.because(
        "We need `@AutoService` for registering custom options provider.")
    ksp(AutoServiceKsp.processor)
    testCompileOnly(AutoService.annotations)
    testImplementation(Time.javaExtensions)
    testImplementation(Logging.testLib)?.because("We need `tapConsole`.")
    testImplementation(project(":testlib"))
    testImplementation(project(":test-env"))

    // The Protobuf runtime, which `tasks.shadowJar` below leaves out of the fat JAR.
    // The `shadow` configuration lists it in the shadowed variant of `compiler-cli`.
    // The POM of `compiler-cli-all` lists it via `declareRuntimeDependencies()`.
    Protobuf.libs.forEach {
        shadow(it) {
            isTransitive = false
        }
    }
}

sourceSets.test {
    kotlin.srcDirs("$projectDir/generated/ksp/test/kotlin")
}

/** The publishing settings from the root project. */
val spinePublishing = rootProject.the<SpinePublishing>()

/**
 * The prefix used when publishing modules.
 */
val modulePrefix = spinePublishing.artifactPrefix

/**
 * Use the same suffix for naming application files as the prefix for published artifacts.
 */
val appName = SpinePublishing.DEFAULT_PREFIX + modulePrefix.replace("-", "")

/** The names of the published modules defined the parent project. */
val modules: Set<String> = spinePublishing.modules

application {
    mainClass.set("io.spine.tools.compiler.cli.app.MainKt")
    applicationName = appName
}

tasks.getByName<CreateStartScripts>("startScripts") {
    windowsStartScriptGenerator = ScriptGenerator { _, _ -> /* Do nothing. */ }
    val template = resources.text.fromFile("$projectDir/launch.template.py")
    (unixStartScriptGenerator as TemplateBasedScriptGenerator).template = template
}

publishing {
    val pGroup = project.group.toString()
    val pVersion = project.version.toString()

    publications {
        create<MavenPublication>("cliFatJar") {
            groupId = pGroup
            artifactId = "compiler-cli-all"
            version = pVersion

            artifact(tasks.shadowJar) {
                // Avoid `-all` suffix in the published artifact.
                // We cannot remove the suffix by setting the `archiveClassifier` for
                // the `shadowJar` task because of the duplication check for pairs
                // (classifier, artifact extension) performed by
                // the `ValidatingMavenPublisher` class.
                classifier = ""
            }
            declareRuntimeDependencies(Protobuf.libs)
        }
    }
}

/**
 * Declares the dependencies on the given `group:name:version` [coordinates]
 * in the POM of this publication, in the `runtime` scope and without
 * transitive dependencies.
 *
 * The publication must not declare dependencies of its own, as `cliFatJar`
 * does not: this function appends a new `dependencies` block to the POM
 * without merging into an existing one.
 */
fun MavenPublication.declareRuntimeDependencies(coordinates: List<String>) {
    pom.withXml {
        val dependencies = asNode().appendNode("dependencies")
        coordinates.forEach {
            // Not `name` and `version`, which would hide those of the publication.
            val (libGroup, libName, libVersion) = it.split(':')
            with(dependencies.appendNode("dependency")) {
                appendNode("groupId", libGroup)
                appendNode("artifactId", libName)
                appendNode("version", libVersion)
                appendNode("scope", "runtime")
                with(appendNode("exclusions").appendNode("exclusion")) {
                    appendNode("groupId", "*")
                    appendNode("artifactId", "*")
                }
            }
        }
    }
}

tasks.publish {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    setup()

    /*
     * Do not bundle the Protobuf runtime. The `cliFatJar` publication declares it
     * as a `runtime` dependency instead.
     *
     * The Compiler runs the plugins of a user project on the same classpath, after
     * this JAR. A bundled copy of the runtime cannot be relocated, as the runtime
     * depends on the names of its classes, and the plugins exchange Protobuf messages
     * with the Compiler. Left unrelocated, the copy shadows the runtime of the user
     * classpath. Should the copy be older than the code generated for the plugins,
     * loading that code fails with `ProtobufRuntimeVersionException`, and no forcing
     * of a version on the user side can replace the copy. Declared as a dependency,
     * the runtime is resolved along with this JAR, so a consumer chooses its version
     * by the standard dependency resolution, as Spine builds do by forcing
     * `Protobuf.libs` in every configuration.
     *
     * Each runtime library is declared non-transitively. Its own dependencies are
     * declared too (`protobuf-java`), or bundled into this JAR (Guava, Gson,
     * the Kotlin runtime). So the classpath gets the same classes as before, except
     * that those of the Protobuf runtime come from genuine artifacts.
     *
     * The runtime libraries are excluded by the dependency filter rather than by
     * the paths of their entries, so that everything they contribute, classes and
     * resources alike, is left out.
     */
    val protobufRuntimeModules = Protobuf.libs.map { it.substringBeforeLast(':') }.toSet()
    dependencies {
        exclude { "${it.moduleGroup}:${it.moduleName}" in protobufRuntimeModules }
    }

    // minimize() ?

    isZip64 = true
    exclude(
        // Exclude license files that cause or may cause issues with LicenseReport.
        // We analyze these files when building artifacts we depend on.
        "about_files/**",
        "license/**",

        "ant_tasks/**", // `resource-ant.jar` is of no use here.

        /* Exclude `https://github.com/JetBrains/pty4j`.
          We don't need the terminal. */
        "resources/com/pty4j/**",

        // Protobuf files.
        "google/**",
        "spine/**",
        "src/**",

        // Java source code files of the package `org.osgi`.
        "OSGI-OPT/**"
    )
}

// See https://github.com/johnrengelman/shadow/issues/153.
tasks.shadowDistTar.get().enabled = false
tasks.shadowDistZip.get().enabled = false
