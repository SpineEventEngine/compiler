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

import com.google.protobuf.RuntimeVersion
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.spine.testing.SlowTest
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.gradle.api.CompilerTaskName
import io.spine.tools.compiler.gradle.api.Names.GRADLE_PLUGIN_ID
import io.spine.tools.gradle.task.TaskName
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.get
import java.io.File
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Verifies that the Protobuf runtime of the JVM forked by a [LaunchSpineCompiler] task
 * is the higher of the versions asked for by the CLI and by the user classpath.
 *
 * The `protobuf-runtime-test` fixture project passes to the `spineCompiler` configuration
 * a Compiler plugin built with the given Protobuf release. The project does not force
 * a Protobuf version.
 *
 * The fixture turns the Protobuf gencode/runtime version check back on in the forked JVM.
 * If the runtime that comes first on the classpath is older than the gencode of the plugin,
 * or of the CLI itself, the Compiler fails with `ProtobufRuntimeVersionException`.
 */
@SlowTest
@DisplayName("`LaunchSpineCompiler` should")
internal class ProtobufRuntimeSpec {

    private val launchSpineCompiler: TaskName = CompilerTaskName(SourceSetName.main)

    private lateinit var projectDir: File

    @BeforeEach
    fun prepareDir(@TempDir projectDir: File) {
        this.projectDir = projectDir
    }

    @Test
    fun `run the Compiler on a newer Protobuf runtime of the user classpath`() {
        withClue("Pick a Protobuf release newer than `$cliProtobuf` for the test.") {
            isNewer(NEWER_PROTOBUF, than = cliProtobuf) shouldBe true
        }

        launchWithPluginOn(NEWER_PROTOBUF)

        // The CLI and the user classpath resolve to the same runtime.
        protobufJavaJars() shouldBe listOf("protobuf-java-$NEWER_PROTOBUF.jar")
    }

    @Test
    fun `keep the Protobuf runtime of the CLI over an older one of the user classpath`() {
        launchWithPluginOn(OLDER_PROTOBUF)

        // The runtime of the CLI comes first and shadows the older one of the plugin.
        protobufJavaJars() shouldBe listOf(
            "protobuf-java-$cliProtobuf.jar",
            "protobuf-java-$OLDER_PROTOBUF.jar",
        )
    }

    /**
     * Creates the fixture project with the plugin built with the given [protobufVersion]
     * and launches the Compiler for the `main` source set.
     */
    private fun launchWithPluginOn(protobufVersion: String) {
        val project = GradleProject.setupAt(projectDir)
            .fromResources("protobuf-runtime-test")
            .withSharedTestKitDirectory()
            .replace("@COMPILER_PLUGIN_ID@", GRADLE_PLUGIN_ID)
            .replace("@COMPILER_VERSION@", Plugin.version)
            .replace("@PLUGIN_PROTOBUF@", protobufVersion)
            .withLoggingLevel(LogLevel.INFO)
            .copyBuildSrc()
            .create()
        project.tuneRunner()

        val result = project.executeTask(launchSpineCompiler)

        result[launchSpineCompiler] shouldBe SUCCESS
    }

    /**
     * Obtains the names of the `protobuf-java` JARs on the classpath of the forked JVM,
     * in the classpath order.
     *
     * The classpath is read from the `compiler-classpath.txt` file written by
     * the fixture build.
     */
    private fun protobufJavaJars(): List<String> =
        projectDir.resolve("compiler-classpath.txt")
            .readText()
            .split(File.pathSeparator)
            .map { File(it).name }
            .filter { it.matches(protobufJavaJar) }
}

/**
 * A Protobuf release newer than the one of this build.
 *
 * It is a release candidate, so that it stays newer than the build after the next
 * update to the latest stable release.
 */
private const val NEWER_PROTOBUF = "4.37.0-RC1"

/**
 * A Protobuf release older than the one of this build.
 */
private const val OLDER_PROTOBUF = "4.33.0"

/**
 * The version of the Protobuf runtime of this build.
 *
 * The CLI declares the same version, as the build forces `Protobuf.libs`
 * in every configuration.
 */
private val cliProtobuf =
    "${RuntimeVersion.MAJOR}.${RuntimeVersion.MINOR}.${RuntimeVersion.PATCH}"

/**
 * Matches the file name of a `protobuf-java` JAR, but not of `protobuf-java-util`.
 */
private val protobufJavaJar = Regex("""protobuf-java-\d.*\.jar""")

/**
 * Tells if the [version] is newer than the version [than] by
 * the major, minor, and patch numbers.
 */
private fun isNewer(version: String, than: String): Boolean {
    fun numbers(v: String) = v.substringBefore('-').split('.').map(String::toInt)
    val difference = numbers(version).zip(numbers(than)).firstOrNull { (a, b) -> a != b }
    return difference != null && difference.first > difference.second
}
