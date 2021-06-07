/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.protodata.gradle

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.gradle.ProtobufPlugin
import java.io.File
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class `Plugin extension should` {

    private lateinit var project: Project
    private lateinit var extension: Extension


    @BeforeEach
    fun prepareProject(@TempDir projectDir: File) {
        project = ProjectBuilder
            .builder()
            .withProjectDir(projectDir)
            .build()
        project.apply(plugin = "java")
        project.apply<ProtobufPlugin>()
        project.apply<Plugin>()

        extension = project.extensions.getByType()
    }

    @Test
    fun `add 'Plugin' class names`() {
        val className = "com.acme.MyPlugin"
        extension.plugins(className)
        assertThat(extension.plugins.get())
            .containsExactly(className)
    }

    @Test
    fun `add 'Renderer' class names`() {
        val className1 = "com.acme.MyRenderer1"
        val className2 = "com.acme.MyRenderer2"
        extension.renderers(className1, className2)
        assertThat(extension.renderers.get())
            .containsExactly(className1, className2)
    }

    @Test
    fun `add dependency tasks`() {
        val taskName = "generateCustomProto"
        extension.generateProtoTasks(taskName)
        assertThat(extension.generateProtoTasks.get())
            .contains(taskName)
    }

    @Test
    fun `specify request file location`() {
        val path = "/my/path/to/file.bin"
        extension.requestFile = path
        assertThat(extension.requestFileProperty.get().asFile)
            .isEqualTo(project.projectDir.resolve(path))
    }
}
