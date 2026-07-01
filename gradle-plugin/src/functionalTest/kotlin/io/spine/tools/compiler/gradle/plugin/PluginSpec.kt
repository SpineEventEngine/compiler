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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.tools.compiler.gradle.api.CompilerTaskName
import io.spine.tools.compiler.gradle.api.Names.GRADLE_PLUGIN_ID
import io.spine.testing.SlowTest
import io.spine.testing.assertDoesNotExist
import io.spine.tools.code.SourceSetName
import io.spine.testing.assertExists
import io.spine.tools.gradle.task.BaseTaskName.build
import io.spine.tools.gradle.task.BaseTaskName.clean
import io.spine.tools.gradle.task.TaskName
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.get
import java.io.File
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@SlowTest
@DisplayName("Spine Compiler Gradle plugin should")
class PluginSpec {

    private val packageDir = "io/spine/tools/compiler/test"

    private val launchSpineCompiler: TaskName = CompilerTaskName(SourceSetName.main)

    private val generateProto: TaskName = TaskName.of("generateProto")

    private lateinit var project: GradleProject
    private lateinit var projectDir: File
    private lateinit var generatedDir: File
    private lateinit var generatedMainDir: File
    private lateinit var generatedJavaDir: File
    private lateinit var generatedKotlinDir: File

    @BeforeEach
    fun prepareDir(@TempDir projectDir: File) {
        this.projectDir = projectDir
        generatedDir = projectDir.resolve("generated")
        generatedMainDir = generatedDir.resolve("main")
        generatedJavaDir = generatedMainDir.resolve("java")
        generatedKotlinDir  = generatedMainDir.resolve("kotlin")
    }

    private fun createProject(resourceDir: String, vararg options: String) {
        val builder = GradleProject.setupAt(projectDir)
            .fromResources(resourceDir)
            .withSharedTestKitDirectory()
            .withOptions(options.toList())
            .replace("@COMPILER_PLUGIN_ID@", GRADLE_PLUGIN_ID)
            .replace("@COMPILER_VERSION@", Plugin.version)
            .withLoggingLevel(LogLevel.INFO)
            /* Uncomment the following if you need to debug the build process.
               Please note that:
                 1) Test will run much slower.
                 2) Under Windows it may cause this issue to occur:
                    https://github.com/gradle/native-platform/issues/274
               After finishing the debug, please comment out this call again. */
            //.enableRunnerDebug()
            .copyBuildSrc()
        project = builder.create()
        (project.runner as DefaultGradleRunner).withJvmArguments(
            "-Xmx8g",
            "-XX:MaxMetaspaceSize=1512m",
            "-XX:+UseParallelGC",
            "-XX:+HeapDumpOnOutOfMemoryError"
        ).withEnvironment(
            mapOf("TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK" to "true")
        )
    }

    private fun createEmptyProject() {
        createProject("empty-test")
    }

    private fun createLaunchTestProject() {
        createProject("launch-test")
    }

    private fun createJavaKotlinProject() {
        createProject("java-kotlin-test")
    }

    /**
     * Verifies that the `test` source set compiles even when the `protoc` output
     * directory ends up among its source directories.
     *
     * The Compiler reads the `protoc` output and writes the processed code into
     * a separate directory, which is added to the source set. The plugin keeps the
     * `protoc` output directory out of the compilation so that each generated class
     * is compiled once. That filtering must hold for the `test` source set too —
     * see [issue #19](https://github.com/SpineEventEngine/compiler/issues/19).
     *
     * The `test-source-set` project re-adds the `protoc` output directory to the
     * `test` source set after the plugin has configured it, reproducing the state
     * in which the directory leaked back in. Without the compilation-level filter
     * the `test` sources contain each generated class twice, and the compilation
     * fails with duplicate class errors.
     */
    @Test
    fun `keep duplicate generated classes out of the 'test' compilation`() {
        createProject("test-source-set")
        val testClasses = TaskName.of("testClasses")

        val result = project.executeTask(testClasses)

        result[CompilerTaskName(SourceSetName.test)] shouldBe SUCCESS
        result[TaskName.of("compileTestJava")] shouldBe SUCCESS
        result[TaskName.of("compileTestKotlin")] shouldBe SUCCESS
    }

    private fun launchAndExpectResult(expected: TaskOutcome) {
        val result = launch()

        val outcome = result[launchSpineCompiler]
        outcome shouldBe expected
    }

    private fun launch(): BuildResult =
        project.executeTask(launchSpineCompiler)

    /**
     * Since there are no `proto` files in this project, the request file is
     * not created, resulting in the [SKIPPED] status of the [launchSpineCompiler] task.
     */
    @Test
    fun `skip launch task if there are no proto files in the project`() {
        createEmptyProject()
        launchAndExpectResult(SKIPPED)
    }

    @Test
    fun `launch the compiler task`() {
        createLaunchTestProject()
        launchAndExpectResult(SUCCESS)
    }

    @Test
    fun `configure incremental compilation for launch task`() {
        createLaunchTestProject()
        launchAndExpectResult(SUCCESS)
        launchAndExpectResult(UP_TO_DATE)
    }

    @Test
    fun `produce 'java' and 'kotlin' directories under 'generated'`() {
        createJavaKotlinProject()
        val result = project.executeTask(build)

        result[build] shouldBe SUCCESS
        assertExists(generatedJavaDir)
        assertExists(generatedKotlinDir)
    }

    @Test
    fun `configure Kotlin compilation`() {
        createProject("kotlin-test")
        val result = project.executeTask(build)

        result[build] shouldBe SUCCESS
        assertExists(generatedKotlinDir)
    }

    @Test
    fun `produce Kotlin code for 'java-library' with 'kotlin(jvm)'`() {
        createProject("java-library-kotlin-jvm")
        val result = project.executeTask(build)

        result[build] shouldBe SUCCESS
        assertExists(generatedKotlinDir)
    }

    @Test
    @Disabled("https://github.com/SpineEventEngine/ProtoData/issues/88")
    fun `add 'kotlin' built-in only' if 'java' plugin or Kotlin compile tasks are present`() {
        createProject("android-library")  // could be in native code
        launchAndExpectResult(SUCCESS)

        assertDoesNotExist(generatedJavaDir)
        assertDoesNotExist(generatedKotlinDir)
    }

    @Test
    fun `support custom source sets`() {
        createProject("with-functional-test")
        val result = project.executeTask(build)

        result[build] shouldBe SUCCESS
        assertExists(generatedKotlinDir)
    }

    @Test
    fun `copy 'grpc' directory from Protobuf's default dir to 'generated'`() {
        createProject("copy-grpc")

        val result = project.executeTask(build)
        result[build] shouldBe SUCCESS

        printFilteredBuildOutput(projectDir, result)

        val parameterClass = "$packageDir/Buz.java"
        assertExists(generatedDir.resolve("main/java/$parameterClass"))

        val serviceClass = "$packageDir/FizServiceGrpc.java"
        assertExists(generatedDir.resolve("main/grpc/$serviceClass"))

        assertExists(generatedJavaDir)
        assertExists(generatedJavaDir.resolve(parameterClass))

        val generatedGrpcDir = generatedMainDir.resolve("grpc")
        assertExists(generatedGrpcDir)
        assertExists(generatedGrpcDir.resolve(serviceClass))
    }

    /**
     * Verifies that the generated code is restored when tasks are taken from
     * the build cache after `clean`.
     *
     * The Compiler `protoc` plugin writes the `CodeGeneratorRequest` file as
     * a side effect of the `generateProto` task. The file is declared as an output
     * of that task so that the build cache restores it along with the generated code.
     * Without the declaration, a `clean build` with the build cache enabled restores
     * `generateProto` from the cache leaving the request file missing, which makes
     * the launch task to be skipped, and the project misses the generated code.
     */
    @Test
    fun `restore the generated code from the build cache after 'clean'`() {
        createProject("cached-build-test", "--build-cache")

        // Guard against stale or shadowed test resources: the copied build script
        // must be the one declaring the Protobuf dependencies required to compile
        // the generated code.
        val buildScript = projectDir.resolve("build.gradle.kts").readText()
        buildScript shouldContain "Protobuf.libs"

        // Seed the build cache.
        project.executeTask(build)
        // Delete `build/` along with the request file, and the `generated/` directory.
        project.executeTask(clean)
        // Both code generation tasks must be restored from the cache.
        val result = project.executeTask(build)

        result[generateProto] shouldBe FROM_CACHE
        result[launchSpineCompiler] shouldBe FROM_CACHE
        assertExists(projectDir.resolve("build/spine/compiler/requests/main.bin"))
        assertExists(generatedJavaDir)
        assertExists(generatedKotlinDir)
    }

    /**
     * Verifies that an incremental build picks up a proto change without `clean`,
     * and that the code generated for the previous version of the proto file
     * does not survive the regeneration.
     *
     * This is the regression test for the historic "always run `clean build`"
     * requirement ([issue #21](https://github.com/SpineEventEngine/compiler/issues/21)):
     * the stale `Test.java` references descriptors that no longer exist after
     * the rename, so the build would fail if the launch task did not clean
     * its output directories before regenerating.
     */
    @Test
    fun `regenerate code and drop stale files after a proto change without 'clean'`() {
        createJavaKotlinProject()
        project.executeTask(build)
        val staleFile = generatedJavaDir.resolve("$packageDir/Test.java")
        assertExists(staleFile)

        val protoFile = projectDir.resolve("src/main/proto/test.proto")
        protoFile.writeText(
            protoFile.readText().replace("message Test {", "message Renamed {")
        )

        val result = project.executeTask(build)

        result[launchSpineCompiler] shouldBe SUCCESS
        assertExists(generatedJavaDir.resolve("$packageDir/Renamed.java"))
        assertDoesNotExist(staleFile)
    }

    /**
     * Verifies that the launch task restores the `generated` directory removed
     * between the builds, while `generateProto` stays `UP_TO_DATE`.
     *
     * No `clean` is required: the inputs of the launch task — produced by
     * `generateProto` and intact under `build/` — suffice for the regeneration.
     */
    @Test
    fun `restore the deleted 'generated' directory without 'clean'`() {
        createJavaKotlinProject()
        project.executeTask(build)
        assertExists(generatedJavaDir)

        generatedDir.deleteRecursively() shouldBe true

        val result = project.executeTask(build)

        result[generateProto] shouldBe UP_TO_DATE
        result[launchSpineCompiler] shouldBe SUCCESS
        assertExists(generatedJavaDir)
        assertExists(generatedKotlinDir)
    }

    /**
     * Verifies that a change unrelated to proto code does not trigger the launch
     * task, and the previously generated code stays in place.
     */
    @Test
    fun `keep the launch task up-to-date when only regular sources change`() {
        createJavaKotlinProject()
        project.executeTask(build)

        val newClass = projectDir.resolve("src/main/java/$packageDir/Unrelated.java")
        newClass.parentFile.mkdirs()
        newClass.writeText("""
            package io.spine.tools.compiler.test;

            /** A class that does not depend on the generated code. */
            final class Unrelated {

                private Unrelated() {
                }
            }
            """.trimIndent()
        )

        val result = project.executeTask(build)

        result[launchSpineCompiler] shouldBe UP_TO_DATE
        assertExists(generatedJavaDir.resolve("$packageDir/Test.java"))
    }

    /**
     * Verifies that settings files rewritten with the same content on every
     * build do not break the up-to-date state of the launch task.
     *
     * Consumer plugins, such as McJava, write settings for the Compiler plugins
     * during the configuration phase of every build. As long as the content
     * stays the same, the launch task must not re-run.
     */
    @Test
    fun `stay up-to-date when settings are rewritten with the same content`() {
        createJavaKotlinProject()
        val buildScript = projectDir.resolve("build.gradle.kts")
        buildScript.appendText(
            """

            // Simulate a consumer plugin writing settings on each configuration run.
            val compilerSettingsDir = file("build/spine/compiler/settings")
            compilerSettingsDir.mkdirs()
            compilerSettingsDir.resolve("custom.plugin.txt").writeText("option: fixed")
            """.trimIndent()
        )

        project.executeTask(build)
        val result = project.executeTask(build)

        result[launchSpineCompiler] shouldBe UP_TO_DATE
    }

    @Test
    fun `make the KSP task depend on the launch task`() {
        createProject("ksp-test")
        val printKspDependencies = TaskName.of("printKspDependencies")

        val result = project.executeTask(printKspDependencies)

        result[printKspDependencies] shouldBe SUCCESS
        val dependenciesLine = result.output.lineSequence()
            .first { it.startsWith("KSP_DEPENDENCIES=") }
        dependenciesLine shouldContain launchSpineCompiler.name()
    }
}

/**
 * Prints console output produced by the build represented by the given [result].
 *
 * The output replaces `projectDir` name with ellipses.
 */
@Suppress("KotlinPrintToLogpoint") // We want the console output in this case.
private fun printFilteredBuildOutput(projectDir: File, result: BuildResult) {
    println("Spine Compiler-related build output:")
    println(
        result.output.split(System.lineSeparator())
            .filter { line -> line.contains("Spine Compiler") }
            .joinToString(System.lineSeparator()) { line ->
                line.replace(projectDir.toString(), "/...")
            }
    )
}
