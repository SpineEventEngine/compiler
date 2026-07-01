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

@file:Suppress("RemoveRedundantQualifierName")

import io.spine.dependency.lib.Grpc
import io.spine.dependency.local.Base
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.Validation
import io.spine.gradle.RunBuild
import io.spine.gradle.publish.PublishingRepos
import io.spine.gradle.publish.SpinePublishing
import io.spine.gradle.publish.spinePublishing
import io.spine.gradle.repo.standardToSpineSdk
import io.spine.gradle.report.coverage.KoverConfig
import io.spine.gradle.report.license.LicenseReporter
import io.spine.gradle.report.pom.PomGenerator
import java.nio.file.Files

buildscript {
    standardSpineSdkRepositories()
    val baseForBuildScript = io.spine.dependency.local.Base.libForBuildScript
    dependencies {
        classpath(io.spine.dependency.lib.Protobuf.GradlePlugin.lib)
        classpath(io.spine.dependency.build.Ksp.run { artifact(gradlePlugin) })
        classpath(io.spine.dependency.local.ToolBase.jvmToolPluginDogfooding)
        classpath(spineCompiler.pluginLib)
        classpath(coreJvmCompiler.pluginLib)
    }
    configurations.all {
        resolutionStrategy {
            force(
                io.spine.dependency.lib.JetBrainsAnnotations.lib,
                io.spine.dependency.lib.Protobuf.javaLib,
                io.spine.dependency.local.Logging.grpcContext,
                io.spine.dependency.local.ToolBase.intellijPlatform,
                io.spine.dependency.local.ToolBase.intellijPlatformJava,
                baseForBuildScript
            )
        }
    }
}

plugins {
    kotlin
    id("org.jetbrains.kotlinx.kover")
    `gradle-doctor`
    `project-report`
    `dokka-setup`
}

/**
 * Publish all the modules, but `gradle-plugin`, which is published separately by its own.
 */
spinePublishing {
    val customPublishing = arrayOf(
        "gradle-plugin"
    )
    modules = productionModuleNames.toSet()
        .minus(customPublishing)

    modulesWithCustomPublishing = customPublishing.toSet()

    destinations = PublishingRepos.run { setOf(
        cloudArtifactRegistry,
        gitHub("compiler")
    )}

    toolArtifactPrefix = "compiler-"
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")
    group = "io.spine.tools"
    version = extra["compilerVersion"]!!

    repositories.standardToSpineSdk()

    configurations.all {
        resolutionStrategy {
            force(
                io.spine.dependency.local.Logging.grpcContext,
                io.spine.dependency.local.ToolBase.intellijPlatform,
                io.spine.dependency.local.ToolBase.intellijPlatformJava,
                Grpc.ProtocPlugin.artifact,
                Reflect.lib,
                Base.lib,
                TestLib.lib,
                CoreJvm.server,
                Validation.runtime,
                Validation.javaBundle
            )
        }
    }
}

PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)
KoverConfig.applyTo(project)

/**
 * Collect `publishToMavenLocal` tasks for all subprojects that are specified for
 * publishing in the root project.
 */
val projectsToPublish: Set<String> = the<SpinePublishing>().modules
val localPublish = tasks.register("localPublish") {
    /*
       Integration tests need the plugin subproject published to Maven Local too
       because they apply the plugin.

       The plugin subproject is not added to the list of `projectsToPublish` because
       it is published from inside its `build.gradle.kts`.
     */
    val includingPlugin = projectsToPublish + "gradle-plugin"
    val pubTasks = includingPlugin.map { p ->
        val subProject = project(p)
        subProject.tasks["publishToMavenLocal"]
    }
    dependsOn(pubTasks)
}

/**
 * Replaces `tests/<linkName>` with a real copy of the root `<linkName>` if
 * the symlink did not survive the checkout.
 *
 * The build under `tests` shares `buildSrc` and `gradle.properties` with this
 * project via relative symlinks. When the repository is checked out without
 * symlink support (e.g., Git with `core.symlinks=false` under Windows), each
 * link turns into a plain text file containing the link target (e.g.,
 * `../buildSrc`). The nested build launched by `integrationTest` then has no
 * `buildSrc` and fails to compile its build scripts with "Unresolved
 * reference" errors. Restoring the linked content keeps the integration tests
 * independent of the symlink handling of the checkout.
 */
fun materializeTestsLink(linkName: String) {
    val link = file("tests/$linkName")
    if (Files.isSymbolicLink(link.toPath())) {
        return
    }
    val placeholder = link.isFile && link.readText().trim() == "../$linkName"
    if (!placeholder) {
        return
    }
    link.delete()
    val target = file(linkName)
    if (target.isDirectory) {
        copy {
            from(target) {
                exclude("build", "build/**", ".gradle", ".gradle/**")
            }
            into(link)
        }
    } else {
        target.copyTo(link)
    }
}

/**
 * The `integrationTest` task runs a Gradle build in the project located
 * under the `tests` subdirectory.
 *
 * This build should run _only_ if all tests of all modules passed.
 * Otherwise, integration tests make little sense.
 */
val integrationTest = tasks.register<RunBuild>("integrationTest") {
    directory = "$rootDir/tests"
    /* The `tests` build consumes the Compiler published to Maven Local by `localPublish`,
       so the build cache in that build exercises the plugin under development. */
    task("clean", "build", "--build-cache")
    dependsOn(localPublish)
    subprojects.forEach {
        it.tasks.findByName("test")?.let { testTask ->
            this@register.dependsOn(testTask)
        }
    }
    doFirst {
        materializeTestsLink("buildSrc")
        materializeTestsLink("gradle.properties")
    }
}

/**
 * The `check` task is done if `integrationTest` passes.
 */
tasks["check"].dependsOn(integrationTest)

/**
 * The below block avoids the version conflict with the `spine-base` used
 * by our Dokka plugin and the module of this project.
 *
 * Here's the error:
 *
 * ```
 * Execution failed for task ':dokkaGeneratePublicationHtml'.
 * > Could not resolve all dependencies for configuration ':dokkaHtmlGeneratorRuntimeResolver~internal'.
 *    > Conflict found for the following module:
 *        - io.spine:spine-base between versions 2.0.0-SNAPSHOT.308 and 2.0.0-SNAPSHOT.309
 * ```
 * The problem is not fixed by forcing the version of [Base.lib] in the block above.
 * It requires the code executed on `afterEvaluate`.
 */
afterEvaluate {
    configurations.named("dokkaHtmlGeneratorRuntimeResolver~internal") {
        resolutionStrategy.preferProjectModules()
    }
}

dependencies {
    /*
       Aggregate the API documentation from all production modules except `test-env`.

       `test-env` calls `disableDocumentationTasks()` (see `test-env/build.gradle.kts`),
       so its per-module Dokka tasks are skipped and it never produces the
       `module-descriptor.json` that the multi-module publication consolidates.
       Including it here makes `:dokkaGeneratePublicationHtml` fail on the missing
       descriptor. Excluding it keeps the aggregation in sync with the modules that
       actually generate documentation.
     */
    productionModules
        .filterNot { it.name == "test-env" }
        .forEach { dokka(it) }
}
