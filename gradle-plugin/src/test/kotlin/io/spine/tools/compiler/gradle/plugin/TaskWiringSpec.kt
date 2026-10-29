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

import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufPlugin
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.gradle.api.CompilerTaskName
import io.spine.tools.compiler.gradle.api.Names.SPINE_COMPILER_PROTOC_PLUGIN
import io.spine.tools.compiler.gradle.api.SpineCompilerCleanTask
import io.spine.tools.compiler.gradle.api.builtins
import io.spine.tools.gradle.project.sourceSets
import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Verifies that the plugin arranges the dependencies between the tasks without making
 * Gradle create them while the project is configured.
 *
 * Each test records the names of the tasks Gradle creates, starting before
 * the plugins are applied. See [issue #98](https://github.com/SpineEventEngine/compiler/issues/98).
 *
 * The end-to-end counterpart, which also covers the Kotlin and Kotlin Symbol Processing (KSP)
 * tasks of the real plugins, is `PluginSpec` in the `functionalTest` suite.
 */
@DisplayName("The plugin should")
internal class TaskWiringSpec {

    private lateinit var projectDir: File
    private lateinit var project: Project

    /**
     * The names of the tasks created by Gradle, in the order of creation.
     */
    private val created = mutableListOf<String>()

    private val launchMain = CompilerTaskName(SourceSetName.main).value()
    private val launchTest = CompilerTaskName(SourceSetName.test).value()

    @BeforeEach
    fun createProject(@TempDir projectDir: File) {
        this.projectDir = projectDir
        project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()
        project.group = "io.spine.stubs"
        project.tasks.configureEach {
            created.add(it.name)
        }
        with(project) {
            apply(plugin = "java")
            apply<ProtobufPlugin>()
            apply<Plugin>()
            repositories.mavenLocal()
        }
    }

    private fun evaluate() = (project as ProjectInternal).evaluate()

    /**
     * Obtains the names of the tasks on which the task with the given [name] depends.
     *
     * Creates the task.
     */
    private fun dependenciesOf(name: String, project: Project = this.project): List<String> {
        val task = project.tasks.getByName(name)
        return task.taskDependencies.getDependencies(task).map { it.name }
    }

    /**
     * Obtains the names of the tasks passed to `dependsOn()` of the task with
     * the given [name], as tasks or task providers.
     *
     * Unlike [dependenciesOf], this function does not resolve other dependencies,
     * such as the build dependencies of the configurations, which would require
     * the Compiler artifacts in Maven Local.
     */
    private fun declaredDependenciesOf(name: String, project: Project = this.project) =
        project.tasks.getByName(name).dependsOn.mapNotNull {
            when (it) {
                is Task -> it.name
                is TaskProvider<*> -> it.name
                else -> null
            }
        }

    @Test
    fun `not create the launch and compilation tasks at configuration time`() {
        // Registered before the project is evaluated, like the KSP plugin does.
        project.tasks.register("kspKotlin")

        evaluate()

        created shouldNotContainAnyOf listOf(
            launchMain,
            launchTest,
            "compileJava",
            "compileTestJava",
            "kspKotlin",
        )
    }

    @Test
    fun `make Java compilation depend on the launch task`() {
        evaluate()

        dependenciesOf("compileJava") shouldContain launchMain
        dependenciesOf("compileTestJava") shouldContain launchTest
    }

    @Test
    fun `make Kotlin compilation depend on the launch task`() {
        evaluate()
        // Stubs of the tasks registered by the Kotlin Gradle Plugin. They are registered
        // after the evaluation, so that the Compiler plugin does not configure the project
        // for Kotlin, which this project does not have.
        project.tasks.register("compileKotlin")
        project.tasks.register("compileTestKotlin")

        dependenciesOf("compileKotlin") shouldContain launchMain
        dependenciesOf("compileTestKotlin") shouldContain launchTest
    }

    @Test
    fun `make the launch task depend on the 'generateProto' task`() {
        evaluate()

        declaredDependenciesOf(launchMain) shouldContain "generateProto"
        declaredDependenciesOf(launchTest) shouldContain "generateTestProto"
    }

    @Test
    fun `pass the proto files compiled by 'generateProto' to the launch task`() {
        val protoFile = projectDir.resolve("src/main/proto/given.proto").canonicalFile
        protoFile.parentFile.mkdirs()
        protoFile.writeText("syntax = \"proto3\";")

        evaluate()

        val launchTask = project.tasks.getByName(launchMain) as LaunchSpineCompiler
        launchTask.protoSourceDirs.asFileTree.files shouldContain protoFile
    }

    @Test
    fun `make 'clean' depend on the clean tasks, which the launch tasks must run after`() {
        evaluate()

        val cleanMain = SpineCompilerCleanTask.nameFor(project.sourceSets.getByName("main"))
        val cleanTest = SpineCompilerCleanTask.nameFor(project.sourceSets.getByName("test"))
        dependenciesOf("clean") shouldContainAll listOf(cleanMain, cleanTest)
        val launchTask = project.tasks.getByName(launchMain)
        launchTask.mustRunAfter.getDependencies(launchTask).map { it.name } shouldContain cleanMain
    }

    @Test
    fun `make the KSP tasks depend on the launch task, even if registered later`() {
        project.tasks.register("kspKotlin")
        evaluate()
        // Registered after the project is evaluated, a case that an `afterEvaluate`
        // callback could not handle.
        project.tasks.register("kspTestKotlin")

        dependenciesOf("kspKotlin") shouldContain launchMain
        dependenciesOf("kspTestKotlin") shouldContain launchTest
    }

    @Test
    fun `serve a source set added after the plugin is applied`() {
        val sourceSet = project.sourceSets.create("integrationTest")
        val launchTask = CompilerTaskName(SourceSetName(sourceSet.name)).value()

        evaluate()

        created shouldNotContainAnyOf listOf(launchTask, sourceSet.compileJavaTaskName)
        project.tasks.names shouldContain launchTask
        dependenciesOf("clean") shouldContain SpineCompilerCleanTask.nameFor(sourceSet)
        dependenciesOf(sourceSet.compileJavaTaskName) shouldContain launchTask
        declaredDependenciesOf(launchTask) shouldContain "generateIntegrationTestProto"
    }

    /**
     * Verifies that the configuration action the plugin adds for each [GenerateProtoTask]
     * runs before the Protobuf Gradle Plugin finalizes the task, and that the code
     * generated by the Compiler reaches the `compileJava` task.
     *
     * The action adds the Compiler `protoc` plugin and the `kotlin` built-in to the task.
     * Once the task is finalized, the Protobuf Gradle Plugin rejects such changes, and
     * the project fails to evaluate. The action also adds the directory with
     * the code generated by the Compiler to the source set.
     */
    @Test
    fun `configure 'generateProto' before it is finalized`() {
        // The temporary directory may be reached via a symbolic link, such as `/var` on macOS.
        // The compilation sources are resolved to canonical paths.
        val generatedFile = projectDir.resolve("generated/main/java/Generated.java")
            .canonicalFile
        generatedFile.parentFile.mkdirs()
        generatedFile.writeText("class Generated {}")

        evaluate()

        val generateProto = project.tasks.getByName("generateProto") as GenerateProtoTask
        generateProto.hasPlugin(SPINE_COMPILER_PROTOC_PLUGIN) shouldBe true
        generateProto.builtins() shouldContain "kotlin"
        val compileJava = project.tasks.getByName("compileJava") as JavaCompile
        compileJava.source.files shouldContain generatedFile
    }

    /**
     * Verifies that the source set directories are configured before the Protobuf Gradle
     * Plugin passes them to the IDEA plugin, which it does right after running the
     * `generateProtoTasks { }` blocks.
     *
     * The configuration of each [GenerateProtoTask] rewrites the source directories of
     * its source set. If it ran only when Gradle creates the task, the IDEA plugin reading
     * the source directories would make Gradle create the task, and the project would fail
     * to evaluate with `ConcurrentModificationException`.
     */
    @Test
    fun `configure 'generateProto' before the IDEA plugin reads the source set`() {
        project.pluginManager.apply("idea")

        evaluate()

        val generateProto = project.tasks.getByName("generateProto") as GenerateProtoTask
        generateProto.hasPlugin(SPINE_COMPILER_PROTOC_PLUGIN) shouldBe true
    }

    @Test
    fun `configure 'generateProto' created before the project is evaluated`() {
        // A build script may create the task before the Protobuf Gradle Plugin
        // runs the `generateProtoTasks { }` blocks.
        project.tasks.getByName("generateProto")

        evaluate()

        val generateProto = project.tasks.getByName("generateProto") as GenerateProtoTask
        generateProto.hasPlugin(SPINE_COMPILER_PROTOC_PLUGIN) shouldBe true
        dependenciesOf("compileJava") shouldContain launchMain
    }

    /**
     * Verifies the wiring when Gradle creates each task as soon as it is registered,
     * as it does for a build calling `tasks.all { }` before applying the plugins.
     *
     * The [LaunchSpineCompiler] task is then configured right when the plugin registers
     * it, and finds the `generateProto` task registered by the Protobuf Gradle Plugin.
     */
    @Test
    fun `wire the tasks created as soon as they are registered`() {
        val eager = ProjectBuilder.builder()
            .withProjectDir(projectDir.resolve("eager"))
            .build()
        eager.group = "io.spine.stubs"
        // Makes Gradle create each task once it is registered.
        eager.tasks.all { }
        with(eager) {
            apply(plugin = "java")
            apply<ProtobufPlugin>()
            apply<Plugin>()
            repositories.mavenLocal()
        }
        val protoFile = eager.projectDir.resolve("src/main/proto/given.proto")
        protoFile.parentFile.mkdirs()
        protoFile.writeText("syntax = \"proto3\";")

        (eager as ProjectInternal).evaluate()

        declaredDependenciesOf(launchMain, eager) shouldContain "generateProto"
        val launchTask = eager.tasks.getByName(launchMain) as LaunchSpineCompiler
        launchTask.protoSourceDirs.asFileTree.files shouldContain protoFile
        dependenciesOf("compileJava", eager) shouldContain launchMain
    }
}
