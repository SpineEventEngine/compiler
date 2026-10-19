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
 * Verifies that [LaunchSpineCompiler] passes the variable disabling the Protobuf
 * version check to the process of the Compiler as configured via
 * [Extension.disableProtobufVersionCheck].
 */
@DisplayName("`LaunchSpineCompiler` should")
internal class LaunchTaskEnvironmentSpec {

    private lateinit var project: Project
    private lateinit var task: LaunchSpineCompiler

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
        val taskName = CompilerTaskName(SourceSetName.main).value()
        // Obtaining the task configures it before the extension is changed by a test,
        // as in a real build, where the task is configured before the `compiler { }` block runs.
        task = project.tasks.getByName(taskName).shouldBeInstanceOf<LaunchSpineCompiler>()
    }

    @Test
    fun `leave the inherited environment intact by default`() {
        // A value other than `true` tells an untouched variable from an overwritten one,
        // regardless of the environment of the machine running the test.
        val inherited = "inherited-from-build"
        task.environment(ENV_VARIABLE, inherited)

        task.disableProtobufVersionCheckIfRequested()

        task.environment[ENV_VARIABLE] shouldBe inherited
    }

    @Test
    fun `disable the Protobuf version check when requested via the extension`() {
        project.compilerSettings.disableProtobufVersionCheck.set(true)

        task.disableProtobufVersionCheckIfRequested()

        task.environment[ENV_VARIABLE] shouldBe "true"
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
