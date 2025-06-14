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

package io.spine.tools.compiler.backend

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.annotation.Internal
import io.spine.code.proto.FileSet
import io.spine.tools.compiler.ast.Coordinates
import io.spine.tools.compiler.ast.Directory
import io.spine.tools.compiler.ast.Documentation
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.toPath
import io.spine.tools.compiler.backend.event.CompilerEvents
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.params.PipelineParameters
import io.spine.tools.compiler.plugin.Plugin
import io.spine.tools.compiler.plugin.applyTo
import io.spine.tools.compiler.plugin.render
import io.spine.tools.compiler.protobuf.ProtoFileList
import io.spine.tools.compiler.protobuf.toPbSourceFile
import io.spine.tools.compiler.render.Renderer
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.compiler.render.SourceFileSet
import io.spine.tools.compiler.settings.SettingsDirectory
import io.spine.tools.compiler.type.TypeSystem
import io.spine.environment.DefaultMode
import io.spine.logging.WithLogging
import io.spine.server.delivery.Delivery
import io.spine.server.storage.memory.InMemoryStorageFactory
import io.spine.server.transport.memory.InMemoryTransportFactory
import io.spine.server.under
import io.spine.string.ti
import io.spine.type.parse
import io.spine.validate.NonValidated
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream

/**
 * A pipeline which processes the Protobuf files.
 *
 * A pipeline consists of the [`Code Generation` context][codegenContext],
 * which receives Protobuf compiler events, and one or more [Plugin]s.
 * The pipeline starts by building [codegenContext] with the supplied [Plugin]s.
 *
 * Then, the Protobuf compiler events are emitted and the subscribers in
 * the context receive them.
 *
 * Then, the [Renderer]s, which are able to query the states of entities
 * in the `Code Generation` context, alter the source set.
 * This may include creating new files, modifying, or deleting existing ones.
 *
 * Lastly, the source set is stored back onto the file system.
 *
 * @property id The ID of the pipeline to be used for distinguishing contexts when
 *   two or more pipelines are executed in the same JVM. If not specified, the ID will be generated.
 * @property params The parameters passed to the pipeline. As the `@NonValidated` annotation
 *   suggests, the given instance may not satisfy all the validation constraints defined in
 *   the `PipelineParameters` message type. This is to allow tests to pass only some of
 *   the parameters if plugins under the test do not need them all.
 *   The production mode of the execution requires a `@Validated` instance of `PipelineParameters`.
 * @param plugins The code generation plugins to be applied to the pipeline in addition to
 *  those specified via [params][PipelineParameters.getPluginClassNameList].
 * @property descriptorFilter The predicate to accept descriptors during parsing of the [request].
 *  The default value accepts all the descriptors.
 *  The primary usage scenario for this parameter is accepting only the descriptors of interest
 *  when running tests.
 */
@Internal
public class Pipeline(
    public val id: String = generateId(),
    public val params: @NonValidated PipelineParameters,
    @VisibleForTesting plugins: List<Plugin> = emptyList(),
    private val descriptorFilter: DescriptorFilter = { true }
) : WithLogging {

    /**
     * Files compiled by `protoc`.
     */
    private val compiledProtoFiles: ProtoFileList by lazy {
        val compiledProtos = params.compiledProtoList.map { it.toPath().toFile() }
        ProtoFileList(compiledProtos)
    }

    /**
     * The Protobuf compiler request loaded from the file specified by
     * the [request property] [PipelineParameters.getRequest] of the [pipeline parameters][params].
     */
    public val request: CodeGeneratorRequest by lazy {
        val requestFile = params.request
        val loadedRequest = if (requestFile == File.getDefaultInstance()) {
            // This is a case of passing partial parameters to a pipeline in tests.
            CodeGeneratorRequest.getDefaultInstance()
        } else {
            // This is a normal production scenario.
            requestFile.toPath().inputStream().use {
                CodeGeneratorRequest::class.parse(it)
            }
        }
        loadedRequest
    }

    /**
     * The directory to which setting files for the [plugins] should be stored.
     */
    public val settings: SettingsDirectory by lazy {
        val dir = params.settings.toPath()
        SettingsDirectory(dir)
    }

    /**
     * The combined list of plugins processed by the pipeline.
     *
     * Contains the plugins loaded by the names of the classes passed via
     * [params][PipelineParameters.getPluginClassNameList] and plugins passed via
     * the constructor parameter.
     */
    public val plugins: List<Plugin> by lazy {
        val combined = loadPlugins(params.pluginClassNameList) + plugins
        combined
    }

    /**
     * The type system passed to the plugins at the start of the pipeline.
     */
    private val typeSystem: TypeSystem by lazy {
        request.toTypeSystem(compiledProtoFiles)
    }

    /**
     * Obtains code generation context used by this pipeline.
     */
    @VisibleForTesting
    public val codegenContext: CodegenContext by lazy {
        assembleCodegenContext()
    }

    /**
     * The source sets to be processed by the pipeline.
     */
    public val sources: List<SourceFileSet> by lazy {
        createSourceFileSets()
    }

    /**
     * Creates a new `Pipeline` with only one plugin and one source set.
     */
    @VisibleForTesting
    public constructor(
        params: PipelineParameters,
        plugin: Plugin,
        id: String = generateId()
    ) : this(id, params, listOf(plugin))

    init {
        under<DefaultMode> {
            use(InMemoryStorageFactory.newInstance())
            use(InMemoryTransportFactory.newInstance())
            use(Delivery.direct())
        }
    }

    private fun loadPlugins(plugins: List<String>): List<Plugin> {
        val classpath = params.userClasspathList.map { Path(it) }
        val factory = PluginFactory(
            Thread.currentThread().contextClassLoader,
            classpath,
            ::printError
        )
        return factory.load(plugins)
    }

    private fun createSourceFileSets(): List<SourceFileSet> {
        checkPaths()
        val sources = params.sourceRootList
        val targets = (params.targetRootList ?: sources)!!
        return sources
            ?.zip(targets)
            ?.map { (s, t) -> s.toPath() to t.toPath() }
            ?.filter { (s, _) -> s.exists() }
            ?.map { (s, t) -> SourceFileSet.create(s, t) }
            ?: targets.oneSetWithNoFiles()
    }

    private fun checkPaths() {
        val sourceRoots = params.sourceRootList
        val targetRoots = params.targetRootList
        if (sourceRoots.isEmpty()) {
            require(targetRoots.size == 1) {
                "When not providing a source directory, only one target directory must be present."
            }
        }
        if (sourceRoots.isNotEmpty()  && targetRoots.isNotEmpty()) {
            require(sourceRoots!!.size == targetRoots!!.size) {
                "Mismatched number of directories." +
                        " Given ${sourceRoots.size} source directories and" +
                        " ${targetRoots.size} target directories."
            }
        }
    }

    /**
     * Executes the processing pipeline.
     *
     * The execution is performed in [Delivery.direct] mode, meaning
     * that no concurrent modification of entity states is allowed.
     * Therefore, the execution of the code related to the signal processing
     * should be single-threaded.
     *
     * @param afterCompile The callback invoked after the compilation process and before
     *  closing [CodegenContext] and other contexts.
     *  The primary purpose of the callback is to allow tests to verify the state
     *  of [CodegenContext], e.g., by [querying][io.spine.server.query.Querying.select]
     *  entity states of interest.
     */
    public operator fun invoke(afterCompile: (CodegenContext) -> Unit = {}) {
        clearCaches()

        logger.atDebug().log { """
            Starting code generation with the following arguments:
              - plugins: ${plugins.joinToString()}
              - request
                  - files to generate: ${request.fileToGenerateList.joinToString()}
                  - parameter: ${request.parameter}.
            """.ti()
        }

        emitEventsAndRenderSources(afterCompile)
    }

    /**
     * Clears the static caches that could have been created by previous runs, e.g., when
     * running from tests.
     *
     * Clears the caches of previously parsed files to avoid repeated code generation.
     * Also, clears the caches of [Documentation] and [Coordinates] classes.
     */
    private fun clearCaches() {
        SourceFile.clearCache()
        Documentation.clearCache()
        Coordinates.clearCache()
    }

    private fun emitEventsAndRenderSources(afterCompile: (CodegenContext) -> Unit) {
        codegenContext.use {
            ConfigurationContext(id).use { configuration ->
                ProtobufCompilerContext(id).use { compiler ->
                    emitEvents(configuration, compiler)
                    renderSources()
                    afterCompile(codegenContext)
                }
            }
        }
    }

    /**
     * Assembles the `Code Generation` context by applying given [plugins].
     */
    private fun assembleCodegenContext(): CodegenContext =
        CodeGenerationContext(id, typeSystem) {
            plugins.forEach {
                it.applyTo(this, typeSystem)
            }
        }

    private fun emitEvents(
        configuration: ConfigurationContext,
        compiler: ProtobufCompilerContext
    ) {
        settings.emitEvents().forEach {
            configuration.emitted(it)
        }
        val events = CompilerEvents.parse(request, typeSystem, descriptorFilter)
        compiler.emitted(events)
    }

    private fun renderSources() {
        plugins.forEach { it.render(codegenContext, sources) }
        sources.forEach { it.write() }
    }

    public companion object {

        /**
         * Generates a random ID for the pipeline.
         *
         * The generated ID is guaranteed to be unique for the current JVM.
         */
        @JvmStatic
        public fun generateId(): String = SecureRandomString.generate()
    }
}

/**
 * Prints the given error [message] to [System.err].
 */
private fun printError(message: String?) {
    System.err.println(message)
}

/**
 * Converts this code generation request into [TypeSystem] taking all the proto files.
 */
@VisibleForTesting
internal fun CodeGeneratorRequest.toTypeSystem(compiledProtoFiles: ProtoFileList): TypeSystem {
    val fileDescriptors = FileSet.of(protoFileList).files()
    val protoFiles = fileDescriptors.map { it.toPbSourceFile() }
    return TypeSystem(compiledProtoFiles, protoFiles.toSet())
}

/**
 * Creates a list that contains a single, empty source set.
 */
private fun List<Directory>.oneSetWithNoFiles(): List<SourceFileSet> =
    listOf(SourceFileSet.empty(first().toPath()))
