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

import com.google.protobuf.gradle.ProtobufPlugin
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.gradle.api.CompilerTaskName
import java.io.File
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Verifies how [LaunchSpineCompiler] controls the variable disabling the Protobuf
 * version check in the environment of the Compiler process.
 *
 * The end-to-end counterpart, with a real version check failing in the Compiler
 * process, is `DisableProtobufVersionCheckSpec` in the `functionalTest` suite.
 */
@DisplayName("`LaunchSpineCompiler` should")
internal class LaunchTaskEnvironmentSpec {

    private lateinit var project: Project

    @BeforeEach
    fun prepareProject(@TempDir projectDir: File) {
        project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()
        with(project) {
            apply(plugin = "java")
            apply<ProtobufPlugin>()
            apply<Plugin>()
        }
    }

    /**
     * Obtains the launch task for the `main` source set, configuring it on first access.
     */
    private fun launchTask(): LaunchSpineCompiler {
        val taskName = CompilerTaskName(SourceSetName.main).value()
        return project.tasks.getByName(taskName).shouldBeInstanceOf<LaunchSpineCompiler>()
    }

    @Test
    fun `apply the setting of the extension when configured`() {
        project.compilerSettings.disableProtobufVersionCheck.set(true)

        launchTask().environment[ENV_VARIABLE] shouldBe "true"
    }

    @Test
    fun `reject a change of the setting after the task is configured`() {
        launchTask()

        shouldThrow<IllegalStateException> {
            project.compilerSettings.disableProtobufVersionCheck.set(true)
        }
    }

    @Test
    fun `leave the inherited environment intact when the setting is not set`() {
        val task = launchTask()
        // A value other than `true` tells an untouched variable from an overwritten one,
        // regardless of the environment of the machine running the test.
        val inherited = "inherited-from-build"
        task.environment(ENV_VARIABLE, inherited)

        task.applyProtobufVersionCheckSetting(null)

        task.environment[ENV_VARIABLE] shouldBe inherited
    }

    @Test
    fun `disable the Protobuf version check when the setting is 'true'`() {
        val task = launchTask()

        task.applyProtobufVersionCheckSetting(true)

        task.environment[ENV_VARIABLE] shouldBe "true"
    }

    @Test
    fun `keep the Protobuf version check when the setting is 'false'`() {
        val task = launchTask()
        // As if the variable were set in the environment of the build.
        task.environment(ENV_VARIABLE, "true")

        task.applyProtobufVersionCheckSetting(false)

        task.environment shouldNotContainKey ENV_VARIABLE
    }

    private companion object {

        /**
         * The name of the variable checked by the Protobuf runtime.
         *
         * The name is repeated here rather than referenced to lock the exact value
         * expected by the Protobuf runtime.
         */
        const val ENV_VARIABLE = "TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK"
    }
}
