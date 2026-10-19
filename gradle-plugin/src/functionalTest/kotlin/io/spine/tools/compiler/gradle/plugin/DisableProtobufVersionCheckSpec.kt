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

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.testing.SlowTest
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.gradle.api.CompilerTaskName
import io.spine.tools.compiler.gradle.api.Names.COMPILER_RAW_ARTIFACT
import io.spine.tools.compiler.gradle.api.Names.GRADLE_PLUGIN_ID
import io.spine.tools.gradle.task.TaskName
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.get
import java.io.File
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Verifies that `compiler { disableProtobufVersionCheck }` controls the Protobuf
 * gencode/runtime version check in the JVM forked by [LaunchSpineCompiler].
 *
 * The tests use the `protobuf-runtime-test` fixture of [ProtobufRuntimeSpec], which turns
 * the check back on in the forked JVM. The plugin of the fixture is built with a Protobuf
 * release newer than the runtime of the CLI. Each test forces the runtime of the CLI
 * configuration, which comes first on the classpath of the forked JVM, to stay at the CLI's
 * own version. So the gencode of the plugin finds a runtime older than itself, as in a build
 * that forces an older Protobuf runtime.
 */
@SlowTest
@DisplayName("`disableProtobufVersionCheck` should")
internal class DisableProtobufVersionCheckSpec {

    private val launchSpineCompiler: TaskName = CompilerTaskName(SourceSetName.main)

    private lateinit var projectDir: File

    @BeforeEach
    fun prepareDir(@TempDir projectDir: File) {
        this.projectDir = projectDir
        withClue("Pick a Protobuf release newer than `$CLI_PROTOBUF` for the test.") {
            isNewer(NEWER_PROTOBUF, than = CLI_PROTOBUF) shouldBe true
        }
    }

    @Test
    fun `keep the check when set to 'false', even if the build disables it`() {
        val project = createProject(
            """
            // Put the variable back, as if it were set in the environment of the build.
            tasks.withType<LaunchSpineCompiler>().configureEach {
                environment("$VERSION_CHECK_VARIABLE", "true")
            }

            spine {
                compiler {
                    disableProtobufVersionCheck.set(false)
                }
            }
            """
        )

        val result = project.executeAndFail(launchSpineCompiler)

        result[launchSpineCompiler] shouldBe FAILED
        result.output shouldContain "ProtobufRuntimeVersionException"
    }

    @Test
    fun `turn the check off when set to 'true'`() {
        val project = createProject(
            """
            spine {
                compiler {
                    disableProtobufVersionCheck.set(true)
                }
            }
            """
        )

        val result = project.executeTask(launchSpineCompiler)

        result[launchSpineCompiler] shouldBe SUCCESS
    }

    /**
     * Creates the fixture project with the plugin built on [NEWER_PROTOBUF].
     *
     * Appends to the build script of the project the code that forces the runtime of
     * the CLI configuration to [CLI_PROTOBUF], followed by the given [caseSetup].
     * A `force` wins over the constraints the Compiler Gradle plugin derives from
     * the user classpath, so the newer runtime of the plugin stays behind the CLI's one
     * on the classpath of the forked JVM.
     *
     * @param caseSetup The build script code setting up the test case.
     */
    private fun createProject(caseSetup: String): GradleProject {
        val project = GradleProject.setupAt(projectDir)
            .fromResources("protobuf-runtime-test")
            .withSharedTestKitDirectory()
            .replace("@COMPILER_PLUGIN_ID@", GRADLE_PLUGIN_ID)
            .replace("@COMPILER_VERSION@", Plugin.version)
            .replace("@PLUGIN_PROTOBUF@", NEWER_PROTOBUF)
            .withLoggingLevel(LogLevel.INFO)
            .copyBuildSrc()
            .create()
        project.tuneRunner()
        val forceCliRuntime = """
            configurations.named("$COMPILER_RAW_ARTIFACT") {
                resolutionStrategy.force("com.google.protobuf:protobuf-java:$CLI_PROTOBUF")
            }
            """
        projectDir.resolve("build.gradle.kts").appendText(
            "\n" + forceCliRuntime.trimIndent() + "\n\n" + caseSetup.trimIndent() + "\n"
        )
        return project
    }
}

/**
 * The name of the variable checked by the Protobuf runtime.
 */
private const val VERSION_CHECK_VARIABLE = "TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK"
