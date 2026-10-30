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

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.gradle.ProtobufPlugin
import io.kotest.matchers.shouldNotBe
import io.spine.tools.compiler.gradle.api.CompilerTaskName
import io.spine.tools.compiler.gradle.api.compilerSettings
import io.spine.tools.code.SourceSetName
import io.spine.tools.gradle.project.sourceSets
import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir

@DisplayName("Plugin configuration should")
class ProjectConfigSpec {

    companion object {

        val mainCompilerTask = CompilerTaskName(SourceSetName.main).value()
        val testCompilerTask = CompilerTaskName(SourceSetName.test).value()

        private lateinit var project: Project

        @Suppress("unused") // False positive: JUnit calls this method.
        @BeforeAll
        @JvmStatic
        fun prepareProject(@TempDir projectDir: File) {
            project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
            project.group = "io.spine.stubs"
            with(project) {
                apply(plugin = "java")
                apply<ProtobufPlugin>()
                apply<Plugin>()
                repositories.mavenLocal()
            }
            with(project.sourceSets) {
                maybeCreate(MAIN_SOURCE_SET_NAME)
                maybeCreate(TEST_SOURCE_SET_NAME)
            }
        }
    }

    @Test
    fun `add 'install' and 'launch' tasks to the project`() {
        println(project.tasks.map { it.name })
        assertThat(project.tasks)
            .comparingElementsUsing(taskNames)
            .containsAtLeast(
                mainCompilerTask,
                testCompilerTask
            )
    }

    @Test
    fun `add extension`() {
        assertDoesNotThrow {
            project.compilerSettings
        }
        project.compilerSettings shouldNotBe null
    }

    @Test
    fun `bind 'launchSpineCompiler' to Java compilation`() {
        val task = project.tasks.getByName("compileJava")
        assertThat(task.taskDependencies.getDependencies(task))
            .comparingElementsUsing(taskNames)
            .contains(CompilerTaskName(SourceSetName.main).value())
    }
}

private val taskNames: Correspondence<Any, String> =
    correspondence("is task named") { something, name ->
        something is Task && something.name == name
    }

private fun <I, O> correspondence(
    description: String,
    predicate: Correspondence.BinaryPredicate<I, O>
): Correspondence<I, O> =
    Correspondence.from(predicate, description)
