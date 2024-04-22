/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.protodata.testing

import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.compiler.codeGeneratorRequest
import io.spine.protodata.FileDependencies
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.settings.SettingsDirectory
import java.nio.file.Path

/**
 * Creates a [Pipeline] for testing the given ProtoData [plugin].
 *
 * This class simulates the first step of the code generation process
 * performed by `protoc` compiler. Since `protoc` is a stable and predictable piece of
 * software, we do not need to go through the "vanilla" code generation process.
 *
 * @param P
 *         the type of the plugin under the test.
 * @property plugin
 *         the plugin to be passed to the created pipeline.
 * @property protoFiles
 *         descriptors of proto files included in the test.
 * @param settingsDir
 *         the directory to which store the settings for the given plugin.
 * @param inputRoot
 *         the root directory with the source code generated by `protoc`.
 * @param outputRoot
 *         the root directory to which the updated code will be placed.
 * @param writeSettings
 *         a callback for writing plugin settings before the pipeline is created.
 *
 * @see [io.spine.protodata.settings.LoadsSettings.consumerId]
 */
public class PipelineSetup<P: Plugin>(
    public val plugin: P,
    public val protoFiles: List<FileDescriptor>,
    settingsDir: Path,
    inputRoot: Path,
    outputRoot: Path,
    private val writeSettings: (P, SettingsDirectory) -> Unit
) {
    /**
     * The directory to store settings for the [plugin].
     */
    public val settings: SettingsDirectory

    /**
     * A sole source file set used by the pipeline.
     */
    public val sourceFileSet: SourceFileSet

    init {
        settingsDir.toFile().mkdirs()
        settings = SettingsDirectory(settingsDir)
        outputRoot.toFile().mkdirs()
        sourceFileSet = SourceFileSet.create(inputRoot, outputRoot)
    }

    /**
     * Creates the pipeline.
     */
    public fun createPipeline(): Pipeline {
        writeSettings(plugin, settings)
        val id = Pipeline.generateId()
        val request = createRequest()
        val pipeline = Pipeline(id, listOf(plugin), listOf(sourceFileSet), request, settings)
        return pipeline
    }

    private fun createRequest() = codeGeneratorRequest {
        fileToGenerate.addAll(protoFiles.map { it.name })

        val dependencies = FileDependencies(protoFiles).asList()
        protoFile.addAll(dependencies.map { it.toProto() })

        val sfd = protoFiles.map { it.toProto() }
        sourceFileDescriptors.addAll(sfd)
    }
}
