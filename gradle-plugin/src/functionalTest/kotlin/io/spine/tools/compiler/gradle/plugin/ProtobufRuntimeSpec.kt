/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
 * a Compiler plugin built with a chosen Protobuf release. The project does not force
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
    fun `run the Compiler on the newer Protobuf runtime of a plugin`() {
        withClue("Pick a Protobuf release newer than `$CLI_PROTOBUF` for the test.") {
            isNewer(NEWER_PROTOBUF, than = CLI_PROTOBUF) shouldBe true
        }

        launchWithPluginOn(NEWER_PROTOBUF)

        // The CLI and the user classpath resolve to the same runtime.
        protobufJavaJars() shouldBe listOf("protobuf-java-$NEWER_PROTOBUF.jar")
    }

    @Test
    fun `not downgrade the Protobuf runtime of the CLI to an older plugin runtime`() {
        launchWithPluginOn(OLDER_PROTOBUF)

        // The CLI runtime comes first and shadows the older plugin runtime.
        protobufJavaJars() shouldBe listOf(
            "protobuf-java-$CLI_PROTOBUF.jar",
            "protobuf-java-$OLDER_PROTOBUF.jar",
        )
    }

    /**
     * Creates the fixture project, whose plugin is built with Protobuf [protobufVersion],
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
     * in classpath order.
     *
     * The classpath is read from the `compiler-classpath.txt` file written by
     * the fixture build.
     */
    private fun protobufJavaJars(): List<String> =
        projectDir.resolve("compiler-classpath.txt")
            .readText()
            .split(File.pathSeparator)
            .map { File(it).name }
            .filter { it.matches(PROTOBUF_JAVA_JAR) }
}

/**
 * A Protobuf release newer than the one used by this build.
 *
 * It is a release candidate of the next minor version, so that it stays newer than
 * the build after the build moves to the latest stable release. Replace it once
 * the build reaches that minor version.
 */
private const val NEWER_PROTOBUF = "4.37.0-RC1"

/**
 * A Protobuf release older than the one used by this build.
 */
private const val OLDER_PROTOBUF = "4.33.0"

/**
 * The version of the Protobuf runtime of this test, without a pre-release suffix.
 *
 * It equals the version the CLI declares: both come from `Protobuf.libs`, which
 * the CLI declares in its POM, and the build forces in every configuration.
 */
private val CLI_PROTOBUF =
    "${RuntimeVersion.MAJOR}.${RuntimeVersion.MINOR}.${RuntimeVersion.PATCH}"

/**
 * Matches the file name of a `protobuf-java` JAR, but not of `protobuf-java-util`.
 */
private val PROTOBUF_JAVA_JAR = Regex("""protobuf-java-\d.*\.jar""")

/**
 * Tells whether the [version] is newer than the [than] version, comparing only
 * the major, minor, and patch numbers. A qualifier such as `-RC1` is ignored.
 */
private fun isNewer(version: String, than: String): Boolean {
    fun numbers(v: String) = v.substringBefore('-').split('.').map(String::toInt)
    val (mine, theirs) = numbers(version).zip(numbers(than))
        .firstOrNull { (a, b) -> a != b }
        ?: return false
    return mine > theirs
}
