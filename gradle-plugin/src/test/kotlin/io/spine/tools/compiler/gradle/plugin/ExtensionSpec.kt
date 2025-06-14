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

package io.spine.tools.compiler.gradle.plugin

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.gradle.ProtobufPlugin
import io.kotest.matchers.shouldBe
import io.spine.tools.gradle.project.sourceSets
import java.io.File
import kotlin.io.path.div
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`Extension` should")
class ExtensionSpec {

    private lateinit var project: Project
    private lateinit var extension: Extension
    
    @BeforeEach
    fun prepareProject(@TempDir projectDir: File) {
        project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        with(project) {
            apply(plugin = "java")
            sourceSets.maybeCreate(MAIN_SOURCE_SET_NAME)
            apply<ProtobufPlugin>()
            apply<Plugin>()

            this@ExtensionSpec.extension = project.compilerSettings
        }
    }

    @Test
    fun `add 'Plugin' class names`() {
        val className = "com.acme.MyPlugin"
        extension.plugins(className)
        assertThat(extension.plugins.get())
            .containsExactly(className)
    }

    @Test
    fun `produce target directory`() {
        val basePath = "my/path"
        val expected = listOf("foo", "bar")

        extension.outputBaseDir.set(project.layout.projectDirectory.dir(basePath))
        extension.subDirs = expected

        val sourceSet = project.sourceSets.getByName(MAIN_SOURCE_SET_NAME)
        val targetDirs = extension.outputDirs(sourceSet).get()

        val mainDir = project.projectDir.toPath() / basePath / MAIN_SOURCE_SET_NAME
        targetDirs[0].toPath() shouldBe mainDir / expected[0]
        targetDirs[1].toPath() shouldBe mainDir / expected[1]
    }
}

private fun Directory.toPath() = asFile.toPath()
