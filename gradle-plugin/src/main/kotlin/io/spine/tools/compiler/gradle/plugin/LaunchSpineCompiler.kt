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
import io.spine.annotation.VisibleForTesting
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.Constants.CLI_APP_CLASS
import io.spine.tools.compiler.ast.toAbsoluteFile
import io.spine.tools.compiler.ast.toDirectory
import io.spine.tools.compiler.gradle.api.Names.COMPILER_RAW_ARTIFACT
import io.spine.tools.compiler.gradle.api.Names.USER_CLASSPATH_CONFIGURATION
import io.spine.tools.compiler.gradle.api.compilerWorkingDir
import io.spine.tools.compiler.gradle.api.error
import io.spine.tools.compiler.gradle.api.info
import io.spine.tools.compiler.params.ParametersFileParam
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.compiler.params.pipelineParameters
import io.spine.tools.gradle.project.findJavaCompileFor
import io.spine.tools.gradle.project.findKotlinCompileFor
import io.spine.tools.protobuf.gradle.containsProtoFiles
import java.io.File
import java.io.File.pathSeparator
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.process.CommandLineArgumentProvider

/**
 * A task that executes a single Spine Compiler command.
 *
 * This class is public to allow users to find the Compiler tasks by their type.
 * This is useful to configure task dependencies, enable and disable individual tasks,
 * add conditions via `onlyIf { }`, etc.
 *
 * Before launching the Compiler, the task deletes its target directories, so that
 * the code generated for the definitions that no longer exist does not survive
 * the regeneration. The cleanup happens only when the task actually executes:
 * a task that is up-to-date, or restored from the build cache, leaves
 * the generated code intact. Incremental builds do not require `clean`.
 *
 * Users should NOT change the CLI command, user directory, etc. directly.
 * Please refer to the `compiler { }` extension configuring the Compiler.
 *
 * ## Attaching instrumentation to the forked JVM
 *
 * The fork options of this task are part of its public contract: JVM arguments
 * a consumer build appends — via the additive [jvmArgs] function or via
 * `jvmArgumentProviders` — are preserved and passed to the forked JVM next to
 * the defaults added by the task itself. Assigning the `jvmArgs` property
 * (`setJvmArgs(...)`) is outside the contract: it replaces the defaults,
 * including the `--add-opens` arguments the Compiler relies on.
 * Consumer builds use the appending forms to attach instrumentation — for
 * example, the JaCoCo agent (`-javaagent:`) capturing the coverage of the
 * Compiler plugins executed inside the fork, or a remote-debug agent.
 * The contract is locked by regression tests.
 *
 * For example, to capture the coverage of the Compiler plugins:
 *
 * ```kotlin
 * val jacocoAgent: Configuration by configurations.creating
 *
 * dependencies {
 *     jacocoAgent("org.jacoco:org.jacoco.agent:<version>:runtime")
 * }
 *
 * tasks.withType<LaunchSpineCompiler>().configureEach {
 *     val execFile = layout.buildDirectory.file("jacoco-compiler/$name.exec")
 *     jvmArgumentProviders.add(CommandLineArgumentProvider {
 *         listOf("-javaagent:${jacocoAgent.singleFile}=destfile=${execFile.get().asFile}")
 *     })
 * }
 * ```
 */
@CacheableTask
public abstract class LaunchSpineCompiler : JavaExec() {

    @get:Input
    internal abstract val sourceSetName: Property<String>

    @get:Input
    internal lateinit var plugins: Provider<List<String>>

    /**
     * The paths to the directories with the generated source code.
     *
     * May not be available if `protoc` built-ins were turned off, resulting in no source code
     * being generated. In that mode, `protoc` produces only descriptor set files.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal lateinit var sources: Provider<List<Directory>>

    /**
     * The file with the serialized `CodeGeneratorRequest` written by the Compiler
     * `protoc` plugin during the run of the `GenerateProtoTask` this task depends on.
     *
     * The file does not exist if the source set contains no proto files.
     * In such a case, the `onlyIf` condition of this task evaluates to `false`
     * (see [hasRequestFile]), and the task is skipped. Inputs of a skipped task
     * are neither validated nor fingerprinted, so the absent file does not
     * violate the [InputFile] contract.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    internal abstract val requestFile: RegularFileProperty

    /**
     * The directory with the settings files consumed by the Compiler plugins.
     *
     * The files are created by the build process before this task runs.
     *
     * The directory is declared via [InputFiles] rather than
     * [InputDirectory][org.gradle.api.tasks.InputDirectory] because it may not
     * exist if no settings were written, while `InputDirectory` requires
     * the directory to exist.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val settingsDirectory: DirectoryProperty

    /**
     * The directories with the proto source files compiled by
     * the [GenerateProtoTask] on which this task depends.
     *
     * The files found under these directories are listed in the parameters
     * file as the compiled proto files.
     *
     * The property is [Internal] because the content of the compiled proto
     * files is already fingerprinted via [requestFile], which the Compiler
     * `protoc` plugin derives from them.
     *
     * @see [consumeProtoFrom]
     */
    @get:Internal
    internal abstract val protoSourceDirs: ConfigurableFileCollection

    /**
     * Tells whether to disable the Protobuf version check in the Compiler process.
     *
     * When not set, the Compiler process inherits the setting from the environment
     * of the build.
     *
     * The property is [Internal] because disabling the check does not affect
     * the generated code: the check either passes or fails the task.
     *
     * @see io.spine.tools.compiler.gradle.api.CompilerSettings.disableProtobufVersionCheck
     */
    @get:Internal
    internal abstract val disableProtobufVersionCheck: Property<Boolean>

    @get:InputFiles
    @get:Classpath
    internal lateinit var userClasspathConfiguration: Configuration

    /**
     * A Gradle [Configuration] that is used to run the Compiler.
     */
    @get:InputFiles
    @get:Classpath
    internal lateinit var compilerConfiguration: Configuration

    /**
     * The paths to the directories where the source code processed by the Compiler should go.
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
     * The file system operations used for cleaning the target directories.
     */
    @get:Inject
    internal abstract val fileSystemOperations: FileSystemOperations

    init {
        // The main class is a constant, so it is set once at configuration time.
        // Doing so at execution time is deprecated and becomes an error in Gradle 11.
        mainClass.set(CLI_APP_CLASS)
        jvmArgs(
            // Open access for Palantir Java Formatter.
            "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        )
    }

    /**
     * Launches the Compiler after cleaning the target directories.
     *
     * The cleanup runs only when the task itself executes: a task that is
     * up-to-date, or restored from the build cache, leaves the previously
     * generated code intact.
     *
     * Applies the Protobuf version check setting of the `compiler { }` extension
     * to the environment of the Compiler process.
     */
    override fun exec() {
        cleanTargetDirs()
        applyProtobufVersionCheckSetting()
        super.exec()
    }

    /**
     * Applies [disableProtobufVersionCheck] to the [PROTOBUF_VERSION_CHECK_ENV]
     * environment variable of the Compiler process.
     *
     * Sets the variable to `true` if the property is `true`, and removes the variable
     * if the property is `false`. When the property is not set, leaves the environment
     * intact, so that the variable set in the environment of the build still reaches
     * the Compiler.
     *
     * The environment is amended when the task executes rather than when it is
     * configured: the property may be set after the task is configured, and Gradle
     * does not evaluate providers passed as environment values. Once Gradle turns
     * the environment of `JavaExec` into a lazy property, the variable can be set
     * at configuration time instead.
     */
    @VisibleForTesting
    internal fun applyProtobufVersionCheckSetting() {
        when (disableProtobufVersionCheck.orNull) {
            true -> environment(PROTOBUF_VERSION_CHECK_ENV, "true")
            false -> environment.remove(PROTOBUF_VERSION_CHECK_ENV)
            null -> {} // Inherit the variable from the environment of the build.
        }
    }

    /**
     * Deletes the existing non-empty [target][targets] directories of this task.
     *
     * Removing the previous output ensures that the code generated for
     * the definitions that no longer exist does not survive the regeneration.
     *
     * A directory that is also a [source][sources] directory is preserved.
     * This protects the deprecated mode in which the Compiler overwrites
     * the code generated by `protoc` in place.
     */
    private fun cleanTargetDirs() {
        val sourceDirs = sources.absoluteDirs()
            .map { it.canonicalFile }
            .toSet()
        targets.absoluteDirs()
            .filter { !it.list().isNullOrEmpty() }
            .filterNot { it.canonicalFile in sourceDirs }
            .forEach { dir ->
                logger.info { "Cleaning target directory `$dir`." }
                fileSystemOperations.delete { it.delete(dir) }
            }
    }
}

/**
 * Applies default settings to the receiver task.
 *
 * Together with the task `init` block and [consumeProtoFrom], this function
 * arranges the whole task state — including the CLI command line — at
 * configuration time. The values that may change until the end of
 * the configuration phase are captured lazily: the classpath via
 * [Configuration]s, and the CLI arguments via `argumentProviders`, which
 * Gradle queries only when building the actual command line.
 * Touching the task state at execution time is deprecated: querying
 * `Task.dependsOn` fails in Gradle 10, changing a property in Gradle 11.
 *
 * The environment of the Compiler process is the only exception: `exec()` amends it
 * via `applyProtobufVersionCheckSetting()`. The environment is a plain map
 * rather than a tracked property, so the amendment is not subject to the deprecation.
 *
 * @param sourceSet The source set for which the task is created.
 */
internal fun LaunchSpineCompiler.applyDefaults(sourceSet: SourceSet) {
    sourceSetName.set(sourceSet.name)
    val project = project
    val ext = project.compilerSettings
    plugins = ext.plugins
    disableProtobufVersionCheck.set(ext.disableProtobufVersionCheck)
    compilerConfiguration = project.compilerRawArtifact
    userClasspathConfiguration = project.userClasspath
    classpath(compilerConfiguration)
    classpath(userClasspathConfiguration)

    sources = ext.sourceDirs(sourceSet)
    targets = ext.outputDirs(sourceSet)

    val ssn = SourceSetName(sourceSet.name)
    requestFile.set(workingDir.requestDirectory.file(ssn))
    settingsDirectory.set(workingDir.settingsDirectory.path.toFile())

    // The parameters file path is passed via an argument provider rather
    // than `args` so that the absolute path does not enter the build cache
    // key, keeping the cached outputs relocatable.
    val parametersFile = workingDir.parametersDirectory.file(ssn)
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(ParametersFileParam.name, parametersFile.path)
    })

    setDependencies(sourceSet)

    onlyIf {
        hasRequestFile(sourceSet)
    }
    doFirst {
        // Writing the parameters file must stay a `doFirst` action rather
        // than move into `exec()`: serializing `PipelineParameters` to JSON
        // loads Spine `KnownTypes` via the thread context classloader, and
        // Gradle sets that classloader to the plugin classloader only while
        // running `doFirst`/`doLast` actions. Inside `exec()` the context
        // classloader is a Gradle core one, and the `desc.ref` resource
        // lookup fails. Unlike the former command-line assembly, this action
        // only writes a file and does not mutate the task state, so it does
        // not trip the "at execution time" deprecations.
        createParametersFile()
    }
}

/**
 * Arranges the dependencies of this task that can be resolved by the time the
 * Compiler plugin is applied.
 *
 * Makes this task depend on the build of the configurations used to run the Compiler,
 * and makes the Java and Kotlin compilation tasks depend on this task.
 *
 * The dependency of the Kotlin Symbol Processing (KSP) task is arranged separately
 * after the project is evaluated because the KSP task is not yet registered by the
 * time the Compiler plugin is applied.
 *
 * @see [Project.arrangeKspTaskDependency][io.spine.tools.compiler.gradle.plugin.arrangeKspTaskDependency]
 */
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
 * Makes this task depend on the given [generateProto] task, and captures
 * the proto source directories compiled by that task
 * into [protoSourceDirs][LaunchSpineCompiler.protoSourceDirs].
 *
 * The directories are captured as a live [file collection][ConfigurableFileCollection]
 * at configuration time, so that the parameters file can be written without
 * querying `Task.dependsOn` at execution time. Such a query is deprecated,
 * fails in Gradle 10, and is incompatible with the configuration cache.
 */
internal fun LaunchSpineCompiler.consumeProtoFrom(generateProto: GenerateProtoTask) {
    dependsOn(generateProto)
    protoSourceDirs.from(generateProto.sourceDirs)
}

/**
 * Writes the file with parameters for a pipeline.
 *
 * The function obtains the list of compiled proto files from
 * [protoSourceDirs][LaunchSpineCompiler.protoSourceDirs] populated from the
 * [GenerateProtoTask] on which the receiver task depends (as arranged by
 * [consumeProtoFrom] called from the `handleLaunchTaskDependency`
 * extension in `Plugin.kt`).
 */
private fun LaunchSpineCompiler.createParametersFile() {
    val params = pipelineParameters {
        val protoFiles = protoSourceDirs.asFileTree.files.toList().sorted()
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
 * Logs an error if the given source set contains a `proto` directory that contains files,
 * which assumes that the request file should have been created.
 */
internal fun LaunchSpineCompiler.hasRequestFile(sourceSet: SourceSet): Boolean {
    val file = requestFile.get().asFile
    if (!file.exists() && sourceSet.containsProtoFiles()) {
        logger.error {
            "Unable to locate the request file `$file` which should have been created" +
                    " because the source set `${sourceSet.name}` contains `.proto` files." +
                    " The task `${name}` was skipped because the absence of the request file."
        }
    }
    return file.exists()
}

/**
 * The name of the environment variable which turns off the check of the code
 * generated by `protoc` against the version of the Protobuf runtime.
 *
 * The Protobuf runtime skips the check if the variable is set to `true`.
 */
private const val PROTOBUF_VERSION_CHECK_ENV = "TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK"

private val Project.compilerRawArtifact: Configuration
    get() = configurations.getByName(COMPILER_RAW_ARTIFACT)

private val Project.userClasspath: Configuration
    get() = configurations.getByName(USER_CLASSPATH_CONFIGURATION)

private fun Provider<List<Directory>>.absoluteDirs(): List<File> = takeIf { it.isPresent }
    ?.get()
    ?.map { it.asFile.absoluteFile }
    ?: listOf()
