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

// Performs many Gradle configuration routines via extension functions.
@file:Suppress("TooManyFunctions")

package io.spine.tools.compiler.gradle.plugin

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableList
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.gradle.GenerateProtoTask
import io.spine.code.proto.DescriptorReference
import io.spine.tools.compiler.gradle.api.Artifacts
import io.spine.tools.compiler.gradle.api.CompilerSettings
import io.spine.tools.compiler.gradle.api.CompilerTask
import io.spine.tools.compiler.gradle.api.Names.COMPILER_RAW_ARTIFACT
import io.spine.tools.compiler.gradle.api.Names.PROTOBUF_GRADLE_PLUGIN_ID
import io.spine.tools.compiler.gradle.api.Names.SPINE_COMPILER_PROTOC_PLUGIN
import io.spine.tools.compiler.gradle.api.Names.USER_CLASSPATH_CONFIGURATION
import io.spine.tools.compiler.gradle.api.ProtocPluginArtifact
import io.spine.tools.compiler.gradle.api.SpineCompilerCleanTask
import io.spine.tools.compiler.gradle.api.compilerWorkingDir
import io.spine.tools.compiler.gradle.api.generatedDir
import io.spine.tools.compiler.gradle.plugin.GeneratedSubdir.GRPC
import io.spine.tools.compiler.gradle.plugin.GeneratedSubdir.JAVA
import io.spine.tools.compiler.gradle.plugin.GeneratedSubdir.KOTLIN
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.string.toBase64Encoded
import io.spine.tools.code.SourceSetName
import io.spine.tools.code.manifest.Version
import io.spine.tools.gradle.lib.LibraryPlugin
import io.spine.tools.gradle.lib.spineExtension
import io.spine.tools.gradle.project.hasJava
import io.spine.tools.gradle.project.hasJavaOrKotlin
import io.spine.tools.gradle.project.hasKotlin
import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.gradle.protobuf.protobufExtension
import io.spine.tools.gradle.task.JavaTaskName
import io.spine.tools.gradle.task.descriptorSetFile
import io.spine.tools.gradle.task.findKotlinDirectorySet
import java.io.File
import java.io.IOException
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.register

/**
 * The Gradle plugin of the Spine Compiler.
 *
 * Adds the `launchSpineCompiler` tasks which runs the executable with the arguments
 * assembled from settings of this plugin.
 *
 * The users can submit configuration parameters, such as renderer and plugin class names, etc. via
 * the `compiler { }` extension.
 *
 * The users can submit the user classpath to the Compiler by declaring dependencies using
 * the `spineCompiler` configuration.
 *
 * Example:
 * ```
 * spine {
 *     compiler {
 *         plugins("com.acme.MyPlugin")
 *     }
 * }
 *
 * dependencies {
 *     spineCompiler(project(":my-plugin"))
 * }
 * ```
 */
public class Plugin : LibraryPlugin<CompilerSettings>(
    CompilerDslSpec()
) {

    init {
        // Inject the access to the project so that `CompilerDslSpec` can
        // create an instance of `Extension`.
        (dslSpec as CompilerDslSpec).project = { this.project }
    }

    /**
     * The version of the plugin.
     */
    private val version: String by lazy {
        readVersion()
    }

    override fun apply(project: Project) {
        super.apply(project)
        createExtension()
        with(project) {
            createConfigurations(this@Plugin.version)
            createTasks()
            configureWithProtobufPlugin(this@Plugin.version)
            configureIdea()
        }
    }

    public companion object {

        /**
         * Reads the version of the plugin from the resources.
         */
        @JvmStatic
        @VisibleForTesting
        public fun readVersion(): String {
            val version = Version.fromManifestOf(Plugin::class.java).value
            return version
        }
    }
}

/**
 * Obtains an instance of the project [Extension] added by the Compiler Gradle Plugin.
 *
 * Or, if the extension is not yet added, creates it and returns.
 */
internal val Project.compilerSettings: Extension
    get() = spineExtension<CompilerSettings>() as Extension

/**
 * Creates configurations for [`spineCompilerRawArtifact`][COMPILER_RAW_ARTIFACT] and
 * user-defined classpath, and adds dependency on [Artifacts.fatCli].
 */
private fun Project.createConfigurations(compilerVersion: String) {
    val artifactConfig = configurations.create(COMPILER_RAW_ARTIFACT) {
        it.isVisible = false
    }
    val cliDependency = Artifacts.fatCli(compilerVersion)
    dependencies.add(artifactConfig.name, cliDependency)

    configurations.create(USER_CLASSPATH_CONFIGURATION) {
        it.exclude(group = Artifacts.group, module = Artifacts.compilerBackend)
    }
}

/**
 * Creates the [SpineCompilerCleanTask] and [LaunchSpineCompiler] tasks for all source sets
 * in this project available by the time of the call.
 *
 * There may be cases of source sets added by other plugins after this function is invoked.
 * Such cases are handled by the [handleLaunchTaskDependency] function.
 *
 * @see [Project.handleLaunchTaskDependency]
 */
private fun Project.createTasks() {
    sourceSets.forEach { sourceSet ->
        createLaunchTask(sourceSet)
        createCleanTask(sourceSet)
    }
}

/**
 * Creates [LaunchSpineCompiler] to serve the given [sourceSet].
 */
@CanIgnoreReturnValue
private fun Project.createLaunchTask(
    sourceSet: SourceSet,
): TaskProvider<LaunchSpineCompiler> {
    val taskName = CompilerTask.nameFor(sourceSet)
    val result = tasks.register<LaunchSpineCompiler>(taskName) {
        applyDefaults(sourceSet)
    }
    return result
}

/**
 * Creates a task which deletes the files generated for the given [sourceSet].
 *
 * Makes a `clean` task depend on the created task.
 * Also, makes the task which launches the Compiler CLI depend on the created task.
 */
private fun Project.createCleanTask(sourceSet: SourceSet) {
    val project = this
    val cleanSourceSet = SpineCompilerCleanTask.nameFor(sourceSet)
    tasks.register<Delete>(cleanSourceSet) {
        delete(compilerSettings.outputDirs(sourceSet))

        val spineCompilerCleanTask = this
        tasks.getByName("clean").dependsOn(spineCompilerCleanTask)
        val compilation = CompilerTask.get(project, sourceSet)
        compilation.mustRunAfter(spineCompilerCleanTask)
    }
}

private fun Project.configureWithProtobufPlugin(compilerVersion: String) {
    val protocPlugin = ProtocPluginArtifact(compilerVersion)
    pluginManager.withPlugin(PROTOBUF_GRADLE_PLUGIN_ID) {
        setProtocPluginArtifact(protocPlugin)
        configureGenerateProtoTasks()
    }
}

/**
 * Configures the Protobuf Gradle Plugin by adding the Compiler plugin
 * to the list of `protoc` plugins.
 */
private fun Project.setProtocPluginArtifact(protocPlugin: ProtocPluginArtifact) {
    protobufExtension?.apply {
        plugins {
            it.create(SPINE_COMPILER_PROTOC_PLUGIN) { locator ->
                locator.artifact = protocPlugin.coordinates
            }
        }
    }
}

/**
 * Configures the `GenerateProtoTaskCollection` by adding a configuration action for
 * each of the tasks.
 */
private fun Project.configureGenerateProtoTasks() {
    protobufExtension?.apply {
        /* The below block adds a configuration action for the `GenerateProtoTaskCollection`.
           We cannot do it like `generateProtoTasks.all().forEach { ... }` because it
           breaks the configuration order of the `GenerateProtoTaskCollection`.
           This, in turn, leads to missing generated sources in the `compileJava` task. */
        generateProtoTasks {
            val all = ImmutableList.copyOf(it.all())
            all.forEach { task ->
                configureProtoTask(task)
            }
        }
    }
}

/**
 * Configures the given [task] by enabling Kotlin code generation and adding and
 * configuring the Compiler `protoc` plugin for the task.
 *
 * The function also handles the exclusion of duplicated source code and task dependencies.
 *
 * @see [GenerateProtoTask.configureSourceSetDirs]
 * @see [Project.handleLaunchTaskDependency]
 */
private fun Project.configureProtoTask(task: GenerateProtoTask) {
    if (hasJavaOrKotlin()) {
        task.builtins.maybeCreate("kotlin")
    }
    task.addProtocPlugin()
    task.configureSourceSetDirs()
    task.setupDescriptorSetFileCreation()
    handleLaunchTaskDependency(task)
}

private fun GenerateProtoTask.addProtocPlugin() {
    plugins.apply {
        create(SPINE_COMPILER_PROTOC_PLUGIN) {
            val requestFile = WorkingDirectory(project.compilerWorkingDir.asFile.toPath())
                .requestDirectory
                .file(SourceSetName(sourceSet.name))
            val path = requestFile.absolutePath
            val nameEncoded = path.toBase64Encoded()
            it.option(nameEncoded)
            if (logger.isDebugEnabled) {
                logger.debug(
                    "The task `$name` got plugin `$SPINE_COMPILER_PROTOC_PLUGIN`" +
                            " with the option `$nameEncoded`."
                )
            }
        }
    }
}

/**
 * The names of the subdirectories where the Compiler places generated files.
 */
private object GeneratedSubdir {
    const val JAVA = "java"
    const val KOTLIN = "kotlin"
    const val GRPC = "grpc"
}

/**
 * Exclude [GenerateProtoTask.outputBaseDir] from Java source set directories to avoid
 * duplicated source code files.
 *
 * Adds the `generated` directory to the Java and Kotlin source sets instead.
 */
private fun GenerateProtoTask.configureSourceSetDirs() {
    val project = project
    val protocOutputDir = File(outputBaseDir).parentFile

    /** Filters out directories belonging to `build/generated/source/proto`. */
    fun excludeFor(lang: SourceDirectorySet) {
        val newSourceDirectories = lang.sourceDirectories
            .filter { !it.residesIn(protocOutputDir) }
            .toSet()

        // Clear the source directories of the Java source set.
        // This trick was needed when building the `base` module of Spine.
        // Otherwise, the `java` plugin would complain about duplicate source files.
        lang.setSrcDirs(listOf<String>())

        // Add the filtered directories back to the Java source set.
        lang.srcDirs(newSourceDirectories)
    }

    val sourceSet = sourceSet

    if (project.hasJava()) {
        val java = sourceSet.java
        excludeFor(java)

        java.srcDir(generatedDir(JAVA))

        // Add the `grpc` directory unconditionally.
        // We may not have all the `protoc` plugins configured for the task at this time.
        // So, we cannot check if the `grpc` plugin is enabled.
        // It is safe to add the directory anyway, because `srcDir()` does not require
        // the directory to exist.
        java.srcDir(generatedDir(GRPC))
    }

    fun SourceDirectorySet.setup() {
        excludeFor(this@setup)
        srcDirs(generatedDir(KOTLIN))
    }

    if (project.hasKotlin()) {
        val kotlinDirectorySet = sourceSet.findKotlinDirectorySet()
        kotlinDirectorySet?.setup()
            ?: project.afterEvaluate {
                sourceSet.findKotlinDirectorySet()?.setup()
            }
    }
}

/**
 * Obtains the `generated` directory for the source set of the task.
 *
 * If [language] is specified returns the subdirectory for this language.
 */
private fun GenerateProtoTask.generatedDir(language: String = ""): File =
    project.generatedDir(sourceSet, language)

/**
 * Tell `protoc` to generate descriptor set files under the project build dir.
 *
 * The name of the descriptor set file to be generated
 * is made to be unique via the project's [Maven coordinates][descriptorSetFile].
 * A unique name is needed for subsequent merging of these files.
 *
 * As the last step of this task, writes a [reference file][DescriptorReference],
 * pointing to the generated descriptor set file.
 * The reference file is used by the Spine Base library and Spine SDK tools
 * when loading the generated descriptor set files.
 */
private fun GenerateProtoTask.setupDescriptorSetFileCreation() {
    isGenerateDescriptorSet = true
    with(descriptorSetOptions) {
        path = descriptorSetFile.path
        isIncludeImports = true
        isIncludeSourceInfo = true
    }

    val descriptorsDir = descriptorSetFile.parentFile

    // Add the `descriptorsDir` directory to the resources.
    project.sourceSets.named(sourceSet.name) {
        it.resources.srcDirs(descriptorsDir)
    }

    setProcessResourceDependency()

    doLast {
        // Create a `desc.ref` in the same resource directory which will be put into resources.
        createDescriptorReferenceFile(descriptorsDir.toPath())
    }
}

/**
 * Makes the `ProcessResources` task of the same source set depend on this task to
 * ensure that descriptor set files and the reference file are picked up as resources.
 */
private fun GenerateProtoTask.setProcessResourceDependency() {
    val taskName = JavaTaskName.processResources(SourceSetName(sourceSet.name))
    project.tasks.named(taskName.value()) {
        it.dependsOn(this)
    }
}

/**
 * Creates a [descriptor reference][DescriptorReference] file in the given directory.
 *
 * Overwrites the file if it already exists.
 */
private fun GenerateProtoTask.createDescriptorReferenceFile(dir: Path) {
    val descRefFile = DescriptorReference.fileAt(dir)
    try {
        descRefFile.writeText(descriptorSetFile.name)
    } catch (e: IOException) {
        project.logger.error("Error writing `${descRefFile.absolutePath}`.", e)
        throw e
    }
}

/**
 * Makes a [LaunchSpineCompiler], if it exists for the source set of the given [GenerateProtoTask],
 * depend on this task.
 *
 * If the [LaunchSpineCompiler] task does not exist (which may be the case for custom source sets
 * created by other plugins), arranges the task creation on [Project.afterEvaluate].
 * In this case the [SpineCompilerCleanTask] is also created with appropriate dependencies.
 */
private fun Project.handleLaunchTaskDependency(generateProto: GenerateProtoTask) {
    val sourceSet = generateProto.sourceSet
    CompilerTask.find(this, sourceSet)
        ?.dependsOn(generateProto)
        ?: afterEvaluate {
            val launchTask = createLaunchTask(sourceSet)
            launchTask.configure {
                it.dependsOn(generateProto)
            }
            createCleanTask(sourceSet)
        }
}
