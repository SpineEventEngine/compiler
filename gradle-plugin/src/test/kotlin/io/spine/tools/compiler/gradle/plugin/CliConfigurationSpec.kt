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

package io.spine.tools.compiler.gradle.plugin

import com.google.protobuf.gradle.ProtobufPlugin
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.spine.tools.compiler.gradle.api.Artifacts
import io.spine.tools.compiler.gradle.api.Names.COMPILER_RAW_ARTIFACT
import io.spine.tools.compiler.gradle.api.Names.USER_CLASSPATH_CONFIGURATION
import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir

/**
 * Verifies how the plugin sets up the configuration of the CLI,
 * [`spineCompilerRawArtifact`][COMPILER_RAW_ARTIFACT].
 *
 * The tests resolve the configuration against a local Maven repository of POMs,
 * in which the CLI and a Compiler plugin depend on different versions of
 * a runtime library. The end-to-end counterpart, with the Protobuf runtime and
 * a forked Compiler JVM, is `ProtobufRuntimeSpec` in the `functionalTest` suite.
 */
@DisplayName("The configuration of the CLI should")
internal class CliConfigurationSpec {

    private lateinit var projectDir: File
    private lateinit var project: Project
    private lateinit var repository: File

    /**
     * Creates the project in the given directory.
     *
     * JUnit does not clean up the directory: under Windows, `ProjectBuilder` keeps
     * a file of the resolution open in the Gradle user home under the directory,
     * and the deletion fails. See
     * [gradle/gradle#12535](https://github.com/gradle/gradle/issues/12535).
     * [deleteProjectDir] removes what it can instead.
     */
    @BeforeEach
    fun prepareProject(@TempDir(cleanup = CleanupMode.NEVER) projectDir: File) {
        this.projectDir = projectDir
        project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()
        repository = projectDir.resolve("repository")
        with(project) {
            apply(plugin = "java")
            apply<ProtobufPlugin>()
            apply<Plugin>()
            // Leave only the local repository, so that the real CLI published to
            // Maven Local or elsewhere does not shadow its POM written by the tests.
            repositories.clear()
            repositories.maven {
                it.setUrl(repository)
                it.metadataSources { sources -> sources.mavenPom() }
            }
        }
    }

    @AfterEach
    fun deleteProjectDir() {
        // Best effort: under Windows, the files kept open by `ProjectBuilder` stay.
        projectDir.deleteRecursively()
    }

    @Test
    fun `require the newer version of a module from the user classpath`() {
        publishCliAndPlugin(cliRuntime = "1.0", pluginRuntime = "2.0")

        resolvedRuntime() shouldBe "2.0"
    }

    @Test
    fun `keep the version of the CLI over an older one from the user classpath`() {
        publishCliAndPlugin(cliRuntime = "1.0", pluginRuntime = "0.9")

        resolvedRuntime() shouldBe "1.0"
    }

    @Test
    fun `let a 'force' rule override the version from the user classpath`() {
        publishCliAndPlugin(cliRuntime = "1.0", pluginRuntime = "2.0")
        project.configurations.getByName(COMPILER_RAW_ARTIFACT)
            .resolutionStrategy
            .force("$RUNTIME:1.0")

        resolvedRuntime() shouldBe "1.0"
    }

    /**
     * The CLI configuration requires the versions resolved in the user classpath.
     * Reading the constraints of the CLI configuration, as a dependency report or
     * an IDE import does, must not resolve the user classpath, which would make it
     * reject the dependencies added later.
     */
    @Test
    fun `not resolve the user classpath when its constraints are read`() {
        val cli = project.configurations.getByName(COMPILER_RAW_ARTIFACT)
        cli.dependencyConstraints.toList()

        assertDoesNotThrow {
            project.dependencies.add(USER_CLASSPATH_CONFIGURATION, PLUGIN)
        }
    }

    /**
     * Publishes the POMs of the CLI and of a plugin, which depend on
     * the given versions of the runtime library, and adds the plugin
     * to the user classpath.
     */
    private fun publishCliAndPlugin(cliRuntime: String, pluginRuntime: String) {
        val cli = Artifacts.fatCli(Plugin.version)
        publishPom(cli, dependency = "$RUNTIME:$cliRuntime")
        publishPom(PLUGIN, dependency = "$RUNTIME:$pluginRuntime")
        publishPom("$RUNTIME:$cliRuntime")
        publishPom("$RUNTIME:$pluginRuntime")
        project.dependencies.add(USER_CLASSPATH_CONFIGURATION, PLUGIN)
    }

    /**
     * Writes the POM of the module with the given [coordinates] into the [repository].
     */
    private fun publishPom(coordinates: String, dependency: String? = null) {
        val (group, name, version) = coordinates.split(':')
        val dependencies = dependency?.split(':')?.let { (depGroup, depName, depVersion) ->
            """
            |  <dependencies>
            |    <dependency>
            |      <groupId>$depGroup</groupId>
            |      <artifactId>$depName</artifactId>
            |      <version>$depVersion</version>
            |    </dependency>
            |  </dependencies>
            """.trimMargin()
        } ?: ""
        val dir = repository.resolve("${group.replace('.', '/')}/$name/$version")
        dir.mkdirs()
        dir.resolve("$name-$version.pom").writeText(
            """
            |<project xmlns="http://maven.apache.org/POM/4.0.0">
            |  <modelVersion>4.0.0</modelVersion>
            |  <groupId>$group</groupId>
            |  <artifactId>$name</artifactId>
            |  <version>$version</version>
            |  <packaging>pom</packaging>
            |$dependencies
            |</project>
            """.trimMargin()
        )
    }

    /**
     * Resolves the CLI configuration and obtains the version of the runtime library in it.
     */
    private fun resolvedRuntime(): String {
        val result = project.configurations.getByName(COMPILER_RAW_ARTIFACT)
            .incoming.resolutionResult
        result.allDependencies
            .filterIsInstance<UnresolvedDependencyResult>()
            .map { "${it.attempted}: ${it.failure.message}" }
            .shouldBeEmpty()
        val modules = result.allComponents.mapNotNull { it.moduleVersion }
        val runtime = withClue("Resolved modules: $modules.") {
            modules.find { "${it.group}:${it.name}" == RUNTIME }.shouldNotBeNull()
        }
        return runtime.version
    }
}

/**
 * The module of a Compiler plugin in the user classpath.
 */
private const val PLUGIN = "com.acme:plugin:1.0"

/**
 * The module of a runtime library on which both the CLI and the plugin depend.
 */
private const val RUNTIME = "com.acme:runtime"
