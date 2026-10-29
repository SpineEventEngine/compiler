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

package io.spine.tools.compiler.gradle.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.spine.tools.gradle.project.sourceSets
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Runs the locators against a project in which the `main` source set has both
 * the launch task and the clean task, so that a locator returning a task of
 * the wrong kind fails its test.
 *
 * The tasks are registered lazily, as the plugin does, so that the locator
 * under test is the first to realize them.
 */
@DisplayName("`TaskLocator` should")
internal class TaskLocatorSpec {

    private val project: Project = ProjectBuilder.builder().build().also {
        it.pluginManager.apply("java")
    }
    private val main: SourceSet = project.sourceSets.getByName(MAIN_SOURCE_SET_NAME)
    private val test: SourceSet = project.sourceSets.getByName(TEST_SOURCE_SET_NAME)
    private val launchTask = project.tasks.register(CompilerTask.nameFor(main))
    private val cleanTask = project.tasks.register(SpineCompilerCleanTask.nameFor(main))

    /**
     * The names of the tasks that Gradle created after the [launchTask] and
     * the [cleanTask] were registered.
     */
    private val created = mutableListOf<String>().also { names ->
        project.tasks.named { it == launchTask.name || it == cleanTask.name }
            .configureEach { names.add(it.name) }
    }

    @Test
    fun `get the launch task via 'CompilerTask'`() {
        CompilerTask.get(project, main) shouldBeSameInstanceAs launchTask.get()
    }

    @Test
    fun `find the launch task via 'CompilerTask'`() {
        CompilerTask.find(project, main) shouldBeSameInstanceAs launchTask.get()
    }

    @Test
    fun `get the clean task via 'SpineCompilerCleanTask'`() {
        SpineCompilerCleanTask.get(project, main) shouldBeSameInstanceAs cleanTask.get()
    }

    @Test
    fun `find the clean task via 'SpineCompilerCleanTask'`() {
        SpineCompilerCleanTask.find(project, main) shouldBeSameInstanceAs cleanTask.get()
    }

    @Test
    fun `find no task for a source set that does not have one`() {
        CompilerTask.find(project, test).shouldBeNull()
        SpineCompilerCleanTask.find(project, test).shouldBeNull()
    }

    @Test
    fun `find no clean task for a source set that has only the launch task`() {
        project.tasks.register(CompilerTask.nameFor(test))
        SpineCompilerCleanTask.find(project, test).shouldBeNull()
    }

    @Test
    fun `fail to get a task for a source set that does not have one`() {
        shouldThrow<UnknownTaskException> { CompilerTask.get(project, test) }
        shouldThrow<UnknownTaskException> { SpineCompilerCleanTask.get(project, test) }
    }

    @Test
    fun `tell if a task exists without creating it`() {
        CompilerTask.exists(project, main) shouldBe true
        SpineCompilerCleanTask.exists(project, main) shouldBe true
        CompilerTask.exists(project, test) shouldBe false
        SpineCompilerCleanTask.exists(project, test) shouldBe false

        created.shouldBeEmpty()
    }

    @Test
    fun `provide a task without creating it`() {
        val launchTaskProvider = CompilerTask.named(project, main)
        val cleanTaskProvider = SpineCompilerCleanTask.named(project, main)

        created.shouldBeEmpty()
        launchTaskProvider.get() shouldBeSameInstanceAs launchTask.get()
        cleanTaskProvider.get() shouldBeSameInstanceAs cleanTask.get()
    }

    @Test
    fun `fail to provide a task for a source set that does not have one`() {
        shouldThrow<UnknownTaskException> { CompilerTask.named(project, test) }
        shouldThrow<UnknownTaskException> { SpineCompilerCleanTask.named(project, test) }
    }
}
