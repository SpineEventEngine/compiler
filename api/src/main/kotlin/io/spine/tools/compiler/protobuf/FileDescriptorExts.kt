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

package io.spine.tools.compiler.protobuf

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors.FileDescriptor
import io.spine.option.OptionsProto
import io.spine.tools.compiler.ast.EnumType
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.ProtoDeclaration
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.ProtoFileHeader.SyntaxVersion
import io.spine.tools.compiler.ast.ProtoFileHeader.SyntaxVersion.PROTO2
import io.spine.tools.compiler.ast.ProtoFileHeader.SyntaxVersion.PROTO3
import io.spine.tools.compiler.ast.ProtobufSourceFile
import io.spine.tools.compiler.ast.Service
import io.spine.tools.compiler.ast.file
import io.spine.tools.compiler.ast.options
import io.spine.tools.compiler.ast.protoFileHeader
import io.spine.tools.compiler.ast.protobufSourceFile

/**
 * Obtains the syntax version of the given [FileDescriptor].
 */
public fun FileDescriptor.syntaxVersion(): SyntaxVersion =
    when (toProto().syntax) {
        "proto2" -> PROTO2
        "proto3" -> PROTO3
        else -> PROTO2
    }

/**
 * Obtains type URL prefix declared in the file.
 *
 * If there is no `type_url_prefix` option declared in the file, assumes that
 * the prefix is `"type.googleapis.com"`, as it's the convention used by Google Protobuf
 * Java implementation for packing `com.google.protobuf.Any` instances.
 */
public val FileDescriptor.typeUrlPrefix: String
    get() {
        val customTypeUrl = options.getExtension(OptionsProto.typeUrlPrefix)
        return if (customTypeUrl.isNullOrBlank()) {
            "type.googleapis.com"
        } else {
            customTypeUrl
        }
    }

/**
 * Obtains a number of imports declared in the proto file represented by this [FileDescriptor].
 *
 * The returned value includes the number of `import public` statements.
 *
 * @see FileDescriptor.dependencies
 * @see FileDescriptor.publicDependencies
 */
public val FileDescriptor.importCount: Int
    get() = dependencies.size

/**
 * Tells if this proto file is imported by [another] file.
 */
public fun FileDescriptor.isImportedBy(another: FileDescriptor): Boolean =
    another.dependencies.contains(this)

/**
 * Obtains the relative path to this file as a [File].
 */
public fun FileDescriptor.file(): File = file { path = name }

/**
 * Extracts metadata from this file descriptor, including file options.
 */
public fun FileDescriptor.toHeader(): ProtoFileHeader = protoFileHeader {
    file = file()
    packageName = `package`
    syntax = syntaxVersion()
    option.addAll(options())
}

/**
 * Obtains the file path from this file descriptor.
 */
public fun FileDescriptorProto.toFile(): File = file {
    path = name
}

/**
 * Converts this file descriptor to the instance of [ProtobufSourceFile].
 */
public fun FileDescriptor.toPbSourceFile(): ProtobufSourceFile {
    val path = file()
    val definitions = DefinitionFactory(this)
    return protobufSourceFile {
        file = path
        header = toHeader()
        with(definitions) {
            type.putAll(messageTypes().associateByUrl())
            enumType.putAll(enumTypes().associateByUrl())
            service.putAll(services().associateByUrl())
        }
    }
}

private fun <T : ProtoDeclaration> Sequence<T>.associateByUrl() =
    associateBy { it.name.typeUrl }

/**
 * A factory of Protobuf definitions of a single `.proto` file.
 *
 * @property file The descriptor of the Protobuf file.
 */
private class DefinitionFactory(private val file: FileDescriptor) {

    /**
     * Builds the message type definitions from the [file].
     *
     * @return all the message types declared in the file, including nested types.
     */
    fun messageTypes(): Sequence<MessageType> {
        var messages = file.messageTypes.asSequence()
        for (msg in file.messageTypes) {
            messages += walkMessage(msg) { it.realNestedTypes() }
        }
        return messages.map { it.toMessageType() }
    }

    /**
     * Builds the enum type definitions from the [file].
     *
     * @return all the enums declared in the file, including nested enums.
     */
    fun enumTypes(): Sequence<EnumType> {
        var enums = file.enumTypes.asSequence()
        for (msg in file.messageTypes) {
            enums += walkMessage(msg) { it.enumTypes }
        }
        return enums.map { it.toEnumType() }
    }

    /**
     * Builds the service definitions from the [file].
     *
     * @return all the services declared in the file, including the nested ones.
     */
    fun services(): Sequence<Service> =
        file.services.asSequence().map { it.toService() }
}
