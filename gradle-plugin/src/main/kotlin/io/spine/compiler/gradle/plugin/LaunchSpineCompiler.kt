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

package io.spine.compiler.gradle.plugin

import com.google.protobuf.gradle.GenerateProtoTask
import com.intellij.openapi.util.io.FileUtil
import io.spine.compiler.Constants.CLI_APP_CLASS
import io.spine.compiler.ast.toAbsoluteFile
import io.spine.compiler.ast.toDirectory
import io.spine.compiler.gradle.Names.COMPILER_RAW_ARTIFACT
import io.spine.compiler.gradle.Names.USER_CLASSPATH_CONFIGURATION
import io.spine.compiler.gradle.error
import io.spine.compiler.gradle.info
import io.spine.compiler.gradle.compilerWorkingDir
import io.spine.compiler.params.ParametersFileParam
import io.spine.compiler.params.WorkingDirectory
import io.spine.compiler.params.pipelineParameters
import io.spine.tools.code.SourceSetName
import io.spine.tools.gradle.project.findJavaCompileFor
import io.spine.tools.gradle.project.findKotlinCompileFor
import io.spine.tools.gradle.protobuf.containsProtoFiles
import java.io.File
import java.io.File.pathSeparator
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.SourceSet

/**
 * A task which executes a single Spine Compiler command.
 *
 * This class is public to allow users to find ProtoData tasks by their type.
 * This is useful to configure task dependencies, enable and disable individual tasks,
 * add conditions via `onlyIf { }`, etc.
 *
 * Users should NOT change the CLI command, user directory, etc. directly.
 * Please refer to the `compiler { }` extension configuring the Compiler.
 */
public abstract class LaunchSpineCompiler : JavaExec() {

    @get:Input
    internal abstract val sourceSetName: Property<String>

    @get:Input
    internal lateinit var plugins: Provider<List<String>>

    /**
     * The paths to the directories with the generated source code.
     *
     * May not be available, if `protoc` built-ins were turned off, resulting in no source code
     * being generated. In such a mode `protoc` worked only generating descriptor set files.
     */
    @get:InputFiles
    @get:Optional
    internal lateinit var sources: Provider<List<Directory>>

    @get:InputFiles
    internal lateinit var userClasspathConfiguration: Configuration

    /**
     * A Gradle [Configuration] which is used to run the Compiler.
     */
    @get:InputFiles
    internal lateinit var compilerConfiguration: Configuration

    /**
     * The paths to the directories where the source code processed by ProtoData should go.
     */
    @get:OutputDirectories
    internal lateinit var targets: Provider<List<Directory>>

    @get:Internal
    internal val workingDir: WorkingDirectory by lazy {
        val dir = project.compilerWorkingDir.asFile
        dir.mkdirs()
        WorkingDirectory(dir.toPath())
    }

    /**
     * Configures the CLI command for this task.
     *
     * This method *must* be called after all the configuration is done for the task.
     */
    internal fun compileCommandLine() {
        val command = sequence {
            val sourceSet = SourceSetName(sourceSetName.get())
            yield(ParametersFileParam.name)
            yield(workingDir.parametersDirectory.file(sourceSet))
        }.asIterable()
        logger.info {
            "Spine Compiler command for `${path}`: ${command.joinToString(separator = " ")}"
        }
        classpath(compilerConfiguration)
        classpath(userClasspathConfiguration)
        mainClass.set(CLI_APP_CLASS)
        args(command)
    }

    internal fun requestPreLaunchCleanup() {
        doFirst(CleanTargetDirs())
    }

    /**
     * Cleans the target directory to prepare it for ProtoData.
     */
    private inner class CleanTargetDirs : Action<Task> {

        override fun execute(t: Task) {
            val sourceDirs = sources.absoluteDirs()
            val targetDirs = targets.absoluteDirs()

            if (sourceDirs.isEmpty()) {
                return
            }
            sourceDirs.asSequence()
                .zip(targetDirs.asSequence())
                .filter { (s, t) ->
                    // Do not clean directories if we are overwriting files in
                    // the directories created by `protoc`.
                    // Such a mode is deprecated currently, but we may revisit this later.
                    !FileUtil.filesEqual(s, t) /* Honor case-sensitivity under macOS. */
                }
                .map { it.second }
                .filter { it.exists() && it.list()?.isNotEmpty() ?: false }
                .forEach {
                    logger.info { "Cleaning target directory `$it`." }
                    project.delete(it)
                }
        }
    }
}

/**
 * Applies default settings to the receiver task.
 *
 * @param sourceSet The source set for which the task is created.
 */
internal fun LaunchSpineCompiler.applyDefaults(sourceSet: SourceSet) {
    sourceSetName.set(sourceSet.name)
    val project = project
    val ext = project.extension
    plugins = ext.plugins
    compilerConfiguration = project.compilerRawArtifact
    userClasspathConfiguration = project.userClasspath

    sources = ext.sourceDirs(sourceSet)
    targets = ext.outputDirs(sourceSet)

    requestPreLaunchCleanup()
    setDependencies(sourceSet)

    onlyIf {
        hasRequestFile(sourceSet)
    }
    doFirst {
        compileCommandLine()
        createParametersFile()
    }
}

private fun LaunchSpineCompiler.setDependencies(sourceSet: SourceSet) {
    val project = project
    dependsOn(
        project.compilerRawArtifact.buildDependencies,
        project.userClasspath.buildDependencies,
    )
    val launchTask = this
    project.findJavaCompileFor(sourceSet)?.dependsOn(launchTask)
    project.findKotlinCompileFor(sourceSet)?.dependsOn(launchTask)
}

/**
 * Writes the file with parameters for a pipeline.
 *
 * The function obtains the list of compiled proto files by querying an instance
 * of [GenerateProtoTask] on which the receiver task depends on (as set by the
 * [Plugin.handleLaunchTaskDependency][io.spine.compiler.gradle.plugin.handleLaunchTaskDependency]
 * function).
 */
private fun LaunchSpineCompiler.createParametersFile() {
    val generateProtoTask = dependsOn.first { it is GenerateProtoTask } as GenerateProtoTask
    val params = pipelineParameters {
        val protoFiles = generateProtoTask.sourceDirs.asFileTree.files.toList().sorted()
            .map {
                it.toAbsoluteFile()
            }
        compiledProto.addAll(protoFiles)
        settings = workingDir.settingsDirectory.path.toAbsolutePath().toDirectory()
        sourceRoot.addAll(
            sources.absoluteDirs().map { it.toDirectory() }
        )
        targetRoot.addAll(
            targets.absoluteDirs().map { it.toDirectory() }
        )
        request = workingDir.requestDirectory
            .file(SourceSetName(sourceSetName.get()))
            .toAbsoluteFile()

        pluginClassName.addAll(plugins.get())

        val ucp = userClasspathConfiguration.asPath.split(pathSeparator)
        userClasspath.addAll(ucp)
    }

    val sourceSet = SourceSetName(sourceSetName.get())
    val file = workingDir.parametersDirectory.write(sourceSet, params)
    logger.info {
        "Parameters file `${file.canonicalPath}` has been created.`"
    }
}

/**
 * Tells if the request file for this task exists.
 *
 * Logs error if the given source set contains `proto` directory which contains files,
 * which assumes that the request file should have been created.
 */
internal fun LaunchSpineCompiler.hasRequestFile(sourceSet: SourceSet): Boolean {
    val requestFile = workingDir.requestDirectory.file(SourceSetName(sourceSet.name))
    if (!requestFile.exists() && sourceSet.containsProtoFiles()) {
        logger.error {
            "Unable to locate the request file `$requestFile` which should have been created" +
                    " because the source set `${sourceSet.name}` contains `.proto` files." +
                    " The task `${name}` was skipped because the absence of the request file."
        }
    }
    return requestFile.exists()
}

private val Project.compilerRawArtifact: Configuration
    get() = configurations.getByName(COMPILER_RAW_ARTIFACT)

private val Project.userClasspath: Configuration
    get() = configurations.getByName(USER_CLASSPATH_CONFIGURATION)

private fun Provider<List<Directory>>.absoluteDirs(): List<File> = takeIf { it.isPresent }
    ?.get()
    ?.map { it.asFile.absoluteFile }
    ?: listOf()
