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

package io.spine.tools.compiler.backend.event

import com.google.common.collect.ImmutableSet
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.GenericDescriptor
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.base.EventMessage
import io.spine.code.proto.FileSet
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.copy
import io.spine.tools.compiler.ast.event.dependencyDiscovered
import io.spine.tools.compiler.ast.event.fileEntered
import io.spine.tools.compiler.ast.event.fileExited
import io.spine.tools.compiler.ast.event.fileOptionDiscovered
import io.spine.tools.compiler.ast.file
import io.spine.tools.compiler.ast.produceOptionEvents
import io.spine.tools.compiler.ast.toJava
import io.spine.tools.compiler.ast.toAbsoluteFile
import io.spine.tools.compiler.backend.DescriptorFilter
import io.spine.tools.compiler.protobuf.file
import io.spine.tools.compiler.protobuf.toHeader
import io.spine.tools.compiler.protobuf.toPbSourceFile
import io.spine.tools.compiler.type.TypeSystem

/**
 * A factory for Protobuf compiler events.
 */
internal object CompilerEvents {

    /**
     * Produces a sequence of events based on the given descriptor set.
     *
     * The sequence is produced lazily. An element is produced only when polled.
     *
     * The resulting sequence is always finite, it's limited by the type set.
     */
    fun parse(
        request: CodeGeneratorRequest,
        typeSystem: TypeSystem,
        descriptorFilter: DescriptorFilter
    ): Sequence<EventMessage> {
        val allFiles = request.protoFileList.toDescriptors()
        val filesToGenerate = request.fileToGenerateList.toSet()
        return sequence {
            val (compiledFiles, dependencies) = allFiles.partition {
                it.name in filesToGenerate
            }
            yieldAll(dependencies.map { it.toDependencyEvent() })
            compiledFiles
                .filter(descriptorFilter)
                .map { ProtoFileEvents(it, typeSystem, descriptorFilter) }
                .forEach {
                    it.apply { produceEvents() }
                }
        }
    }
}

/**
 * Produces events from the associated file.
 */
private class ProtoFileEvents(
    private val file: FileDescriptor,
    typeSystem: TypeSystem,
    private val descriptorFilter: DescriptorFilter
) {
    /**
     * The header of the proto [file] passed to the constructor.
     *
     * During the lazy evaluation, the [ProtoFileHeader.file][ProtoFileHeader.getFile] property,
     * which is relative by default, is replaced with the absolute version using
     * the [TypeSystem] passed to the constructor.
     *
     * If the full path of the proto file is not available via the [TypeSystem],
     * the header with the relative path is used.
     */
    private val header: ProtoFileHeader by lazy {
        val hdr = file.toHeader()
        val relativePath = hdr.file.toJava()
        val fullPath = typeSystem.compiledProtoFiles.find(relativePath)
        if (fullPath != null) {
            hdr.copy { file = fullPath.toAbsoluteFile() }
        } else {
            hdr
        }
    }

    /**
     * Yields compiler events for the given file.
     *
     * Opens with an [FileEntered][io.spine.tools.compiler.ast.event.FileEntered] event.
     * Then go the events regarding the file metadata.
     * Then go the events regarding the file contents.
     * At last, closes with an [FileExited][io.spine.tools.compiler.ast.event.FileExited] event.
     */
    suspend fun SequenceScope<EventMessage>.produceEvents() {
        yield(
            fileEntered {
                // Avoid the name clash with the class property.
                val hdr = this@ProtoFileEvents.header
                file = hdr.file
                header = hdr
            }
        )
        produceOptionEvents(file.options, file) {
            fileOptionDiscovered {
                file = header.file
                option = it
            }
        }
        val messageEvents = MessageEvents(header)
        file.messageTypes.forEachFiltered {
            messageEvents.apply { produceEvents(it) }
        }
        val enumEvents = EnumEvents(header)
        file.enumTypes.forEachFiltered {
            enumEvents.apply { produceEvents(it) }
        }
        val serviceEvents = ServiceEvents(header)
        file.services.forEachFiltered {
            serviceEvents.apply { produceEvents(it) }
        }
        yield(
            fileExited {
                file = header.file
            }
        )
    }

    private inline fun <T : GenericDescriptor> List<T>.forEachFiltered(
        action: (T) -> Unit) = filter(descriptorFilter).forEach(action)
}

/**
 * Convert this collection of [FileDescriptorProto] to a set of corresponding
 * instances of [FileDescriptor].
 */
private fun Collection<FileDescriptorProto>.toDescriptors(): ImmutableSet<FileDescriptor> {
    val files = FileSet.of(this)
    val fileDescriptors = files.files()
    return fileDescriptors
}

/**
 * Creates a `DependencyDiscovered` event from the given file descriptor.
 *
 * The event reflects all the definitions from the file.
 */
private fun FileDescriptor.toDependencyEvent() =
    dependencyDiscovered {
        file = file()
        source = toPbSourceFile()
    }
