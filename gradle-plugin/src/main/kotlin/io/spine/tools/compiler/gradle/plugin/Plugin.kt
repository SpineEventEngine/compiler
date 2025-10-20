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

import com.google.common.collect.ImmutableList
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.gradle.GenerateProtoTask
import io.spine.annotation.VisibleForTesting
import io.spine.string.toBase64Encoded
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.gradle.api.Artifacts
import io.spine.tools.compiler.gradle.api.Artifacts.compilerBackend
import io.spine.tools.compiler.gradle.api.Artifacts.compilerGradlePlugin
import io.spine.tools.compiler.gradle.api.Artifacts.protobufProtocArtifact
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
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.gradle.lib.LibraryPlugin
import io.spine.tools.gradle.lib.spineExtension
import io.spine.tools.gradle.project.hasJavaOrKotlin
import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.meta.ArtifactMeta
import io.spine.tools.meta.MavenArtifact
import io.spine.tools.protobuf.gradle.GeneratedDirectoryContext
import io.spine.tools.protobuf.gradle.plugin.DescriptorSetFilePlugin
import io.spine.tools.protobuf.gradle.protobufExtension
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.register

import io.spine.tools.protobuf.gradle.plugin.configureSourceSetDirs

/**
 * The Gradle plugin of the Spine Compiler.
 *
 * Adds the `launchSpineCompiler` tasks which runs the executable with the arguments
 * assembled from settings of this plugin.
 *
 * The users can submit configuration parameters, such as renderer and plugin class
 * names, etc. via the `compiler { }` extension.
 *
 * The user classpath to the Compiler can be passed by declaring dependencies
 * using the `spineCompiler` configuration.
 *
 * Example (`build.gradle.kts`):
 *
 * ```kotlin
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
), GeneratedDirectoryContext {

    init {
        // Inject the access to the project so that `CompilerDslSpec` can
        // create an instance of `Extension`.
        (dslSpec as CompilerDslSpec).project = { this.project }
    }

    override fun apply(project: Project) {
        super.apply(project)
        createExtension()
        val pluginVersion = version
        project.run {
            createConfigurations(pluginVersion)
            setProtocArtifact()
            createTasks()
            configureWithProtobufPlugin(pluginVersion)
            configureIdea()
        }
    }

    override fun generatedDir(
        project: Project,
        sourceSet: SourceSet,
        language: String
    ): Path = project.generatedDir.resolve("${sourceSet.name}/$language")

    public companion object {

        /**
         * The meta-data of the [Plugin] loaded from resources.
         */
        private val meta by lazy {
            ArtifactMeta.loadFromResource(compilerGradlePlugin, this::class.java)
        }

        /**
         * The version of the [Plugin] loaded from resources.
         */
        @VisibleForTesting
        public val version: String by lazy {
            meta.version
        }

        /**
         * Reads the version of the plugin from the resources.
         */
        @JvmStatic
        @VisibleForTesting
        public fun readVersion(): String {
            return meta.version
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
    val artifactConfig = configurations.create(COMPILER_RAW_ARTIFACT)
    val cliDependency = Artifacts.fatCli(compilerVersion)
    dependencies.add(artifactConfig.name, cliDependency)

    configurations.create(USER_CLASSPATH_CONFIGURATION) {
        it.exclude(group = compilerBackend.group, module = compilerBackend.name)
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

private fun Project.setProtocArtifact() {
    val artifactMeta = ArtifactMeta.loadFromResource(
        compilerGradlePlugin,
        Plugin::class.java.classLoader
    )
    val protocArtifact = artifactMeta.dependencies.find(protobufProtocArtifact) as? MavenArtifact
    checkNotNull(protocArtifact) {
        "Unable to load `protoc` dependency of `${Plugin::class.qualifiedName}`."
    }
    protobufExtension!!.protoc { locator ->
        locator.artifact = protocArtifact.coordinates
    }
}

context(_: GeneratedDirectoryContext)
private fun Project.configureWithProtobufPlugin(compilerVersion: String) {
    val protocPlugin = ProtocPluginArtifact(compilerVersion)
    pluginManager.withPlugin(PROTOBUF_GRADLE_PLUGIN_ID) {
        afterEvaluate {
            pluginManager.apply(DescriptorSetFilePlugin::class.java)
        }
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
context(_: GeneratedDirectoryContext)
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
context(_: GeneratedDirectoryContext)
private fun Project.configureProtoTask(task: GenerateProtoTask) {
    if (hasJavaOrKotlin()) {
        task.builtins.maybeCreate("kotlin")
    }
    task.addProtocPlugin()
    task.configureSourceSetDirs()
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
