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

// Performs many Gradle configuration routines via extension functions.
@file:Suppress("TooManyFunctions")

package io.spine.tools.compiler.gradle.plugin

import com.google.common.collect.ImmutableList
import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufExtension
import io.spine.annotation.VisibleForTesting
import io.spine.format.Format
import io.spine.io.replaceExtension
import io.spine.string.toBase64Encoded
import io.spine.tools.code.Kotlin
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.gradle.api.Artifacts
import io.spine.tools.compiler.gradle.api.Artifacts.compilerBackend
import io.spine.tools.compiler.gradle.api.Artifacts.compilerGradlePlugin
import io.spine.tools.compiler.gradle.api.Artifacts.protobufGroup
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
import io.spine.tools.gradle.project.hasKotlin
import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.gradle.task.SpineTaskGroup
import io.spine.tools.meta.ArtifactMeta
import io.spine.tools.meta.MavenArtifact
import io.spine.tools.protobuf.gradle.GeneratedDirectoryContext
import io.spine.tools.protobuf.gradle.plugin.DescriptorSetFilePlugin
import io.spine.tools.protobuf.gradle.plugin.configureSourceSetDirs
import io.spine.tools.protobuf.gradle.protobufExtension
import java.io.File
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * The Gradle plugin of the Spine Compiler.
 *
 * Adds the `launchSpineCompiler` task that runs the executable with the arguments
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
 * user-defined classpath, and adds a dependency on [Artifacts.fatCli].
 *
 * The `spineCompilerRawArtifact` configuration requires the Protobuf runtime resolved
 * in the [`spineCompiler`][USER_CLASSPATH_CONFIGURATION] configuration.
 * See [requireProtobufRuntimeOf].
 */
private fun Project.createConfigurations(compilerVersion: String) {
    val artifactConfig = configurations.create(COMPILER_RAW_ARTIFACT)
    val cliDependency = Artifacts.fatCli(compilerVersion)
    dependencies.add(artifactConfig.name, cliDependency)

    val userClasspath = configurations.create(USER_CLASSPATH_CONFIGURATION) {
        it.exclude(group = compilerBackend.group, module = compilerBackend.name)
    }
    requireProtobufRuntimeOf(userClasspath, target = artifactConfig)
}

/**
 * Makes the [target] configuration require the version of each Protobuf module
 * resolved in the [source] configuration.
 *
 * [LaunchSpineCompiler] puts the CLI before the user classpath, so that the modules
 * bundled into the CLI JAR take precedence. The CLI declares the Protobuf runtime
 * instead of bundling it. The version the CLI declares must not shadow a newer one
 * that the gencode of the Compiler plugins on the user classpath needs.
 *
 * The function adds a dependency constraint on each module of the Protobuf group
 * ([protobufGroup]) resolved in the [source]. Gradle's conflict resolution then picks the higher of
 * the versions asked for by the CLI and by the [source]. A `force` rule applied to
 * the [target] still wins. A `strictly` version or a dependency lock lower than
 * the version of the [source] fails the resolution, as such a runtime would fail
 * the gencode of the plugins.
 *
 * Other modules of the [source] are left alone. The user classpath may hold any
 * library that a Compiler plugin brings, for example, Spine Validation used by
 * the CoreJvm Compiler. The Protobuf runtime is the only part of it that the CLI
 * shares by design.
 *
 * `shouldResolveConsistentlyWith` is not suitable here: it locks the versions with
 * `strictly`, so it would downgrade the runtime of the CLI to an older one
 * from the user classpath.
 *
 * The constraints are added right before the [target] is resolved, so the [source]
 * remains open for new dependencies until then. Adding them resolves the [source].
 */
private fun Project.requireProtobufRuntimeOf(source: Configuration, target: Configuration) {
    val constraints = dependencies.constraints
    val reason = "Protobuf runtime resolved in `${source.name}`"
    target.withDependencies {
        source.incoming.resolutionResult.allComponents
            .map { it.id }
            .filterIsInstance<ModuleComponentIdentifier>()
            .filter { it.group == protobufGroup }
            .forEach { id ->
                constraints.add(target.name, "${id.group}:${id.module}:${id.version}") {
                    it.because(reason)
                }
            }
    }
}

/**
 * Registers the [SpineCompilerCleanTask] and [LaunchSpineCompiler] tasks for each source set
 * of this project, including the source sets added by other plugins after this call.
 *
 * Gradle creates none of the tasks by this call. Making other tasks depend on
 * the [LaunchSpineCompiler] tasks creates no tasks either.
 *
 * @see [Project.makeCodeTasksDependOn]
 */
private fun Project.createTasks() {
    sourceSets.configureEach { sourceSet ->
        val launchTask = createLaunchTask(sourceSet)
        makeCodeTasksDependOn(sourceSet, launchTask)
        createCleanTask(sourceSet, launchTask)
    }
}

/**
 * Makes the tasks processing the code of the given [sourceSet] depend on
 * the [launchTask], so that the Compiler runs before them.
 *
 * These are the Java and Kotlin compilation tasks and the Kotlin Symbol Processing (KSP) task.
 *
 * The tasks are matched by name and configured once Gradle creates them, so this call
 * creates no tasks. The match also covers the tasks registered after this call, such as
 * the KSP task, which the KSP plugin registers after the Compiler plugin is applied.
 */
private fun Project.makeCodeTasksDependOn(
    sourceSet: SourceSet,
    launchTask: TaskProvider<LaunchSpineCompiler>,
) {
    val dependents = setOf(
        sourceSet.compileJavaTaskName,
        sourceSet.getCompileTaskName(Kotlin.name),
        KspTaskName.of(sourceSet).value()
    )
    tasks.named { it in dependents }.configureEach {
        it.dependsOn(launchTask)
    }
}

/**
 * Creates [LaunchSpineCompiler] to serve the given [sourceSet].
 */
private fun Project.createLaunchTask(
    sourceSet: SourceSet,
): TaskProvider<LaunchSpineCompiler> {
    val taskName = CompilerTask.nameFor(sourceSet)
    val result = tasks.register<LaunchSpineCompiler>(taskName) {
        group = SpineTaskGroup.name
        description = "Launch the Spine Compiler for the `${sourceSet.name}` source set"
        applyDefaults(sourceSet)
    }
    return result
}

/**
 * Creates a task that deletes the files generated for the given [sourceSet].
 *
 * Makes the `clean` task depend on the created task, and orders the [launchTask]
 * to run after it when both are scheduled.
 */
private fun Project.createCleanTask(
    sourceSet: SourceSet,
    launchTask: TaskProvider<LaunchSpineCompiler>,
) {
    val cleanSourceSet = SpineCompilerCleanTask.nameFor(sourceSet)
    val spineCompilerCleanTask = tasks.register<Delete>(cleanSourceSet) {
        group = SpineTaskGroup.name
        description =
            "Delete files generated by the Spine Compiler for the `${sourceSet.name}` source set"
        delete(compilerSettings.outputDirs(sourceSet))
    }
    // Wire both edges outside the configuration action above: it runs only once
    // the task is realized, which nothing else guarantees.
    tasks.named("clean") {
        it.dependsOn(spineCompilerCleanTask)
    }
    launchTask.configure {
        it.mustRunAfter(spineCompilerCleanTask)
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
    protobufExtensionOrFail().protoc { locator ->
        locator.artifact = protocArtifact.coordinates
    }
}

/**
 * Obtains the extension of the Protobuf Gradle Plugin applied to this project.
 *
 * @throws IllegalStateException if the Protobuf Gradle Plugin (`com.google.protobuf`)
 *  is not applied to the project. The Spine Compiler plugin requires it.
 */
internal fun Project.protobufExtensionOrFail(): ProtobufExtension =
    checkNotNull(protobufExtension) {
        "The Spine Compiler plugin requires the Protobuf Gradle Plugin" +
            " (`$PROTOBUF_GRADLE_PLUGIN_ID`) to be applied to the project `$path`." +
            " Please add `id(\"$PROTOBUF_GRADLE_PLUGIN_ID\")` to the `plugins` block."
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
        excludeProtocOutputFromCompilation()
    }
}

/**
 * Excludes the `protoc` output directory from the input of the Java and Kotlin
 * compilation tasks of this project.
 *
 * The Compiler takes the code generated by `protoc` as its input and writes the
 * processed code into a separate directory (`generated` by default). The plugin
 * adds that directory to the source sets. The `protoc` output directory
 * ([protocOutputDir]) must be kept out of the compilation: otherwise each
 * generated class is compiled twice — once from the `protoc` output and once
 * from the Compiler output — and the build fails with duplicate class errors.
 *
 * The plugin already removes the `protoc` directories from the source sets via
 * [configureSourceSetDirs][io.spine.tools.protobuf.gradle.plugin.configureSourceSetDirs].
 * That removal rewrites the source-set directories once during configuration and
 * does not always hold for the `test` source set: depending on the order in which
 * the plugins are applied, a symlinked project path, or another plugin re-adding
 * the directory, the `protoc` output may end up back in the source set. This
 * function adds an order-independent line of defence by filtering the compilation
 * input directly. The filter is evaluated when a compilation task resolves its
 * sources, so it holds regardless of when the directory was added to the source
 * set.
 *
 * The deprecated in-place mode, in which the Compiler overwrites the `protoc`
 * output instead of writing to a separate directory, is left untouched: there the
 * generated code legitimately resides in the `protoc` output directory, so
 * filtering it out would drop it from compilation.
 *
 * @see [GenerateProtoTask.configureSourceSetDirs]
 */
private fun Project.excludeProtocOutputFromCompilation() {
    afterEvaluate {
        // Canonicalize the `protoc` output directory once and reuse it while
        // filtering each source file, instead of re-canonicalizing it per file.
        val protocOutput = protocOutputDir.canonicalFile.toPath()
        val compilerWritesInPlace = generatedDir.toFile().residesIn(protocOutput)
        if (compilerWritesInPlace) {
            return@afterEvaluate
        }
        // `JavaCompile` ignores an `exclude(Spec)` predicate for the files it
        // passes to `javac`, so its source is re-set to a filtered view of
        // itself. The view wraps the task's own (live) source and preserves the
        // task dependencies it carries, so the generated code is still produced
        // before compilation.
        tasks.withType(JavaCompile::class.java).configureEach { task ->
            task.source = task.source.filter { file ->
                !file.residesIn(protocOutput)
            }.asFileTree
        }
        // `KotlinCompile` honors `exclude(Spec)` — a live predicate that needs no
        // source re-wiring. The cast targets `KotlinCompile` (which is a
        // `PatternFilterable`); any other `KotlinCompilationTask` is skipped.
        //
        // The Kotlin Gradle Plugin is a `compileOnly` dependency, so its task type
        // is not on the runtime classpath. The reference to `KotlinCompilationTask`
        // is therefore guarded by `hasKotlin()` (a name-based check), keeping the
        // plugin loadable for consumers that apply it without Kotlin.
        if (hasKotlin()) {
            val underProtocOutput = Spec<FileTreeElement> { element ->
                element.file.residesIn(protocOutput)
            }
            tasks.withType(KotlinCompilationTask::class.java).configureEach { task ->
                (task as? PatternFilterable)?.exclude(underProtocOutput)
            }
        }
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
           We cannot call `generateProtoTasks.all().forEach { ... }` outside the block
           because it breaks the configuration order of the `GenerateProtoTaskCollection`.
           This, in turn, leads to missing generated sources in the `compileJava` task.

           Inside the block, the tasks are configured right away rather than via `configureEach`.
           Configuring a task rewrites the source directories of its source set
           (see `configureSourceSetDirs()`). That must happen before the Protobuf Gradle
           Plugin passes the source directories to the IDEA plugin, right after running
           this block. Otherwise, reading the directories makes Gradle create the task,
           and the rewrite breaks the iteration over the directories with
           `ConcurrentModificationException`. If the IDEA plugin is applied, the Protobuf
           Gradle Plugin creates the tasks at that point anyway.

           The result of `all()` is copied so that configuring the tasks does not iterate
           over a live collection. */
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
 * The function also handles the exclusion of duplicated source code.
 *
 * The dependency of the [LaunchSpineCompiler] task on the given [task] is arranged
 * by the [LaunchSpineCompiler] task itself.
 *
 * @see [GenerateProtoTask.configureSourceSetDirs]
 */
context(_: GeneratedDirectoryContext)
private fun Project.configureProtoTask(task: GenerateProtoTask) {
    if (hasJavaOrKotlin()) {
        task.builtins.maybeCreate("kotlin")
    }
    task.addProtocPlugin()
    task.configureSourceSetDirs()
}

private fun GenerateProtoTask.addProtocPlugin() {
    val requestFile = WorkingDirectory(project.compilerWorkingDir.asFile.toPath())
        .requestDirectory
        .file(SourceSetName(sourceSet.name))
    plugins.apply {
        create(SPINE_COMPILER_PROTOC_PLUGIN) {
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
    declareRequestFileOutputs(requestFile)
}

/**
 * The name of the [GenerateProtoTask] output property for the request file.
 */
private const val REQUEST_FILE_PROPERTY = "spineCompilerRequestFile"

/**
 * The name of the [GenerateProtoTask] output property for the JSON twin of the request file.
 */
private const val REQUEST_FILE_JSON_PROPERTY = "spineCompilerRequestFileJson"

/**
 * Declares the [requestFile] written by the Compiler `protoc` plugin — and its JSON
 * twin produced by `CodeGeneratorRequestWriter` — as outputs of this task.
 *
 * The Compiler `protoc` plugin writes these files as a side effect of running `protoc`.
 * Unless they are declared as task outputs, the build cache does not store them, and
 * a task restored from the cache leaves the request file missing. The [LaunchSpineCompiler]
 * task is then skipped (see [hasRequestFile]), and no code is generated.
 */
private fun GenerateProtoTask.declareRequestFileOutputs(requestFile: File) {
    outputs.file(requestFile)
        .withPropertyName(REQUEST_FILE_PROPERTY)
    outputs.file(requestFile.replaceExtension(Format.ProtoJson.extension))
        .withPropertyName(REQUEST_FILE_JSON_PROPERTY)
}
