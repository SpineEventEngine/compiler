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

package io.spine.tools.compiler.gradle.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
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
}
