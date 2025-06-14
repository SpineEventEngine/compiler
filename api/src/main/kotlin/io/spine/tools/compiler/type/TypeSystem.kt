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

package io.spine.tools.compiler.type

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.Descriptors.FileDescriptor
import io.spine.base.FieldPath
import io.spine.base.isNotNested
import io.spine.base.root
import io.spine.base.stepInto
import io.spine.tools.compiler.ast.EnumType
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.ProtoDeclaration
import io.spine.tools.compiler.ast.ProtoDeclarationName
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.ProtobufSourceFile
import io.spine.tools.compiler.ast.Service
import io.spine.tools.compiler.ast.ServiceName
import io.spine.tools.compiler.ast.TypeBase
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.field
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.ast.toJava
import io.spine.tools.compiler.ast.typeName
import io.spine.tools.compiler.protobuf.ProtoFileList
import io.spine.tools.compiler.protobuf.file
import io.spine.type.shortDebugString
import java.io.File

/**
 * A collection of known Protobuf types.
 *
 * @property compiledProtoFiles The list of Protobuf files compiled by `protoc`.
 * @property definitions The result of parsing of files compiled by `protoc` and files they import.
 */
public class TypeSystem(
    public val compiledProtoFiles: ProtoFileList,
    private val definitions: Set<ProtobufSourceFile>
) {
    /**
     * Looks up a message type by its name.
     */
    public fun findMessage(name: TypeName): Pair<MessageType, ProtoFileHeader>? =
        find(name) { it.typeMap }

    /**
     * Looks up an enum type by its name.
     */
    public fun findEnum(name: TypeName): Pair<EnumType, ProtoFileHeader>? =
        find(name) { it.enumTypeMap }

    /**
     * Looks up a message or enum type by its name.
     */
    public fun findMessageOrEnum(name: TypeName): Pair<ProtoDeclaration, ProtoFileHeader>? =
        findMessage(name) ?: findEnum(name)

    /**
     * Looks up a service by its name.
     */
    public fun findService(name: ServiceName): Pair<Service, ProtoFileHeader>? =
        find(name) { it.serviceMap }

    private fun <T> find(
        name: ProtoDeclarationName,
        mapSelector: (ProtobufSourceFile) -> Map<String, T>
    ): Pair<T, ProtoFileHeader>? {
        val typeUrl = name.typeUrl
        val file = definitions.find {
            mapSelector(it).containsKey(typeUrl)
        }
        val types = file?.let(mapSelector)
        val type = types?.get(typeUrl)
        return if (type != null) type to file.header else null
    }
}

/**
 * Obtains an absolute path of the proto file in which the given declaration is made.
 *
 * If the proto declaration already refers to an absolute file, it is returned.
 * Otherwise, the type system attempts to find it among
 * [compiledProtoFiles][TypeSystem.compiledProtoFiles].
 *
 * @return the file with the declaration, or `null` if the file is not among
 *  the [compiled proto files][TypeSystem.compiledProtoFiles] known to this type system.
 */
@VisibleForTesting
public fun TypeSystem.absoluteFileOf(d: ProtoDeclaration): File? {
    val file = d.file.toJava()
    if (file.isAbsolute) {
        return file
    }
    return compiledProtoFiles.find(file)
}

/**
 * Obtains the full path of the proto file specified with the given descriptor.
 *
 * @return the absolute file, or `null` if the file is not among
 *  the [compiled proto files][TypeSystem.compiledProtoFiles] known to this type system.
 */
@VisibleForTesting
public fun TypeSystem.findAbsolute(f: FileDescriptor): File? {
    return compiledProtoFiles.find(f.file().toJava())
}

/**
 * Finds a header of the file which declares the given [type].
 *
 * @return the header for the Protobuf file in which the given type is declared, or
 *         `null` if the header was not found.
 * @throws IllegalArgumentException
 *          if the given type is not a message or an enum.
 */
public fun TypeSystem.findHeader(type: TypeBase): ProtoFileHeader? {
    require(type.isMessage || type.isEnum) {
        "The type must be either a message or an enum. Passed: `${type.shortDebugString()}`."
    }
    val typeName = type.typeName
    val found = when {
        type.isMessage -> findMessage(typeName)
        type.isEnum -> findEnum(typeName)
        else -> null // Cannot happen.
    }
    return found?.second
}

/**
 * Resolves the given [FieldPath] against the given [MessageType] within this [TypeSystem].
 *
 * This method recursively navigates through the nested messages and fields as specified by
 * the [fieldPath], returning the final [Field] that the path points to.
 *
 * @param fieldPath The field path to resolve.
 * @param message The message where the root of the [fieldPath] is declared.
 * @throws IllegalStateException if one of the components of the field path represents
 *  a non-message message field. Or, if the type system could not find an instance of
 *  [MessageType] referenced by the field type.
 */
public fun TypeSystem.resolve(fieldPath: FieldPath, message: MessageType): Field {
    val currentField = message.field(fieldPath.root)
    if (fieldPath.isNotNested) {
        return currentField
    }

    check(currentField.type.isMessage) {
        "Can't resolve the field path `$fieldPath` because `${currentField.name}` segment" +
                " does not denote a message field. The type of this field is" +
                " `${currentField.type}`. Only messages can have nested field."
    }

    val currentFieldMessage = currentField.type.message
    val nextMessageInfo = findMessage(currentFieldMessage)
    check(nextMessageInfo != null) {
        "`${currentFieldMessage.qualifiedName}` was not found in the passed proto files" +
                " or their dependencies."
    }
    val remainingPath = fieldPath.stepInto()
    val nextMessage = nextMessageInfo.first
    return resolve(remainingPath, nextMessage)
}

/**
 * Obtains a message type by its name.
 *
 * @throws IllegalArgumentException if the requested type is unknown for this [TypeSystem].
 */
public fun TypeSystem.message(name: TypeName): MessageType = findMessage(name)?.first
    ?: error("Message type `${name.qualifiedName}` not found in the `TypeSystem`.")

/**
 * Obtains an enum type by its name.
 *
 * @throws IllegalArgumentException if the requested type is unknown for this [TypeSystem].
 */
public fun TypeSystem.enum(name: TypeName): EnumType = findEnum(name)?.first
    ?: error("Enum type `${name.qualifiedName}` not found in the `TypeSystem`.")

/**
 * Obtains a message or enum type by its name.
 *
 * @throws IllegalArgumentException if the requested type is unknown for this [TypeSystem].
 */
public fun TypeSystem.messageOrEnum(name: TypeName): ProtoDeclaration =
    findMessageOrEnum(name)?.first
        ?: error("Message or enum type `${name.qualifiedName}` not found in the `TypeSystem`.")

/**
 * Obtains a service by its name.
 *
 * @throws IllegalArgumentException if the requested type is unknown for this [TypeSystem].
 */
public fun TypeSystem.service(name: ServiceName): Service = findService(name)?.first
    ?: error("Service type `${name.qualifiedName}` not found in the `TypeSystem`.")
