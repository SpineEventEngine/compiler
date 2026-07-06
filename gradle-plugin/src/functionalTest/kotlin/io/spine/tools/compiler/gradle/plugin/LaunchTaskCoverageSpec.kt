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

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.spine.testing.SlowTest
import io.spine.testing.assertExists
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.gradle.api.CompilerTaskName
import io.spine.tools.compiler.gradle.api.Names.GRADLE_PLUGIN_ID
import io.spine.tools.gradle.task.TaskName
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.get
import java.io.File
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.tools.ExecFileLoader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Verifies that a consumer build can attach the JaCoCo agent to the JVM forked
 * by a [LaunchSpineCompiler] task and capture the coverage of the Compiler
 * plugins executed inside that JVM.
 *
 * The Compiler plugins named in the `compiler { plugins(...) }` block run in
 * the forked JVM, out of reach of the coverage agent of a consumer's `test`
 * task. The supported way to measure their coverage is via the standard fork
 * options of [JavaExec][org.gradle.api.tasks.JavaExec]: the
 * `coverage-agent-test` fixture project adds `-javaagent:<jacocoagent>` via
 * `jvmArgumentProviders`. The in-process counterpart of this contract is
 * `LaunchTaskJvmArgsSpec` in the `test` suite.
 *
 * This spec locks three facts at once:
 *  1. Consumer-supplied JVM arguments reach the forked JVM (the agent runs and
 *     writes execution data).
 *  2. The plugin classes from the `spineCompiler` configuration are executed in
 *     the fork as-is, not relocated: the recorded class IDs match the jars the
 *     fork loaded, so the JaCoCo report shows covered lines.
 *  3. An up-to-date launch does not erase previously collected data.
 */
@SlowTest
@DisplayName("`LaunchSpineCompiler` should")
internal class LaunchTaskCoverageSpec {

    private val launchSpineCompiler: TaskName = CompilerTaskName(SourceSetName.main)

    private lateinit var project: GradleProject
    private lateinit var projectDir: File

    @BeforeEach
    fun prepareProject(@TempDir projectDir: File) {
        this.projectDir = projectDir
        val builder = GradleProject.setupAt(projectDir)
            .fromResources("coverage-agent-test")
            .withSharedTestKitDirectory()
            .replace("@COMPILER_PLUGIN_ID@", GRADLE_PLUGIN_ID)
            .replace("@COMPILER_VERSION@", Plugin.version)
            .withLoggingLevel(LogLevel.INFO)
            .copyBuildSrc()
        project = builder.create()
        project.tuneRunner()
    }

    @Test
    fun `let a consumer capture coverage of plugins executed in the forked JVM`() {
        val result = project.executeTask(launchSpineCompiler)
        result[launchSpineCompiler] shouldBe SUCCESS

        // The agent attached via `jvmArgumentProviders` wrote execution data.
        val execFile = projectDir.resolve(
            "build/jacoco-compiler/${launchSpineCompiler.name()}.exec"
        )
        assertExists(execFile)
        execFile.length() shouldBeGreaterThan 0L

        // The fork executed the plugin classes, and the agent recorded that.
        val loader = ExecFileLoader().apply { load(execFile) }
        val rendererData = loader.executionDataStore.contents.find {
            it.name == RENDERER_CLASS
        }.shouldNotBeNull()
        rendererData.hasHits() shouldBe true

        // The execution data matches the classes of the jar the fork loaded
        // the plugins from — class IDs align — and shows covered lines.
        val coverage = analyzeUserClasspath(loader)
        val renderer = coverage.classes.find {
            it.name == RENDERER_CLASS
        }.shouldNotBeNull()
        renderer.lineCounter.coveredCount shouldBeGreaterThan 0

        // An up-to-date launch must leave the collected data intact.
        val secondRun = project.executeTask(launchSpineCompiler)
        secondRun[launchSpineCompiler] shouldBe UP_TO_DATE
        assertExists(execFile)
    }

    /**
     * Runs the JaCoCo [Analyzer] over the `compiler-test-env` jar resolved by
     * the fixture project, matching it against the loaded execution data.
     *
     * The jar path comes from the `user-classpath.txt` file written by the
     * fixture build, so the analysis runs over the exact artifact the forked
     * JVM had on its classpath.
     */
    private fun analyzeUserClasspath(loader: ExecFileLoader): CoverageBuilder {
        val userClasspath = projectDir.resolve("user-classpath.txt").readText()
        val testEnvJar = userClasspath.split(File.pathSeparator)
            .map(::File)
            .find { it.name.startsWith("compiler-test-env") }
            .shouldNotBeNull()
        val coverage = CoverageBuilder()
        Analyzer(loader.executionDataStore, coverage).analyzeAll(testEnvJar)
        return coverage
    }
}

/**
 * The VM name of the test renderer class executed inside the forked JVM.
 */
private const val RENDERER_CLASS = "io/spine/tools/compiler/test/UnderscorePrefixRenderer"
