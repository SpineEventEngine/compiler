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

import io.spine.dependency.lib.AutoService
import io.spine.dependency.lib.AutoServiceKsp
import io.spine.dependency.lib.Clikt
import io.spine.dependency.local.Logging
import io.spine.gradle.publish.SpinePublishing
import io.spine.gradle.publish.handleMergingServiceFiles

plugins {
    module
    application
    `version-to-resources`
    `write-manifest`
    `build-proto-model`
    `maven-publish`
    prototap
    id("com.gradleup.shadow")
}

dependencies {
    listOf(
        kotlin("reflect"),
        Clikt.lib,
        Logging.lib,
        Logging.libJvm,
    ).forEach { implementation(it) }

    listOf(
        ":api",
        ":params",
        ":backend",
        ":jvm"
    ).forEach { implementation(project(it)) }

    testAnnotationProcessor(AutoService.processor)?.because(
        "We need `@AutoService` for registering custom options provider.")
    ksp(AutoServiceKsp.processor)
    testCompileOnly(AutoService.annotations)
    testImplementation(Logging.testLib)?.because("We need `tapConsole`.")
    testImplementation(project(":testlib"))
    testImplementation(project(":test-env"))
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
        }
    }
}

tasks.publish {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    handleMergingServiceFiles()

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

// Set explicit dependency for the `kspTestKotlin` task to avoid the Gradle warning
// on missing explicit dependency.
project.afterEvaluate {
    val kspTestKotlin by tasks.getting
    val launchTestProtoData by tasks.getting
    kspTestKotlin.dependsOn(launchTestProtoData)
}
