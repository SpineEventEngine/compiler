/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.protodata

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type.BOOL
import com.google.protobuf.Descriptors.FieldDescriptor.Type.BYTES
import com.google.protobuf.Descriptors.FieldDescriptor.Type.DOUBLE
import com.google.protobuf.Descriptors.FieldDescriptor.Type.ENUM
import com.google.protobuf.Descriptors.FieldDescriptor.Type.FIXED32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.FIXED64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.FLOAT
import com.google.protobuf.Descriptors.FieldDescriptor.Type.GROUP
import com.google.protobuf.Descriptors.FieldDescriptor.Type.INT32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.INT64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SFIXED32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SFIXED64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SINT32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SINT64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.STRING
import com.google.protobuf.Descriptors.FieldDescriptor.Type.UINT32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.UINT64
import com.google.protobuf.Descriptors.FileDescriptor.Syntax
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Empty
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.base.EventMessage
import io.spine.code.proto.FileSet
import io.spine.option.OptionsProto
import io.spine.protobuf.TypeConverter
import io.spine.protodata.File.SyntaxVersion
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.PrimitiveType.TYPE_INT32
import io.spine.protodata.PrimitiveType.TYPE_INT64
import io.spine.protodata.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.PrimitiveType.TYPE_SINT32
import io.spine.protodata.PrimitiveType.TYPE_SINT64
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.PrimitiveType.TYPE_UINT32
import io.spine.protodata.PrimitiveType.TYPE_UINT64

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
    fun parse(request: CodeGeneratorRequest): Sequence<EventMessage> {
        val filesToGenerate = request.fileToGenerateList.toSet()
        val files = FileSet.of(request.protoFileList)
        return sequence {
            files.files()
                 .filter { it.name in filesToGenerate }
                 .forEach { produceFileEvents(it) }
        }
    }

    /**
     * Yields compiler events for the given file.
     *
     * Opens with an [FileEntered] event. Then go the events regarding the file metadata. Then go
     * the events regarding the file contents. At last, closes with an [FileExited] event.
     */
    private suspend fun
            SequenceScope<EventMessage>.produceFileEvents(descriptor: Descriptors.FileDescriptor) {
        val path = descriptor.path()
        val file = File
            .newBuilder()
            .setPath(path)
            .setPackageName(descriptor.`package`)
            .setSyntax(descriptor.syntax.toSyntaxVersion())
            .build()
        yield(
            FileEntered
                .newBuilder()
                .setFile(file)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            FileOptionDiscovered
                .newBuilder()
                .setFile(path)
                .setOption(it)
                .build()
        }
        descriptor.messageTypes.forEach { produceMessageEvents(file, it) }
        yield(
            FileExited
                .newBuilder()
                .setFile(path)
                .build()
        )
    }

    /**
     * Yields compiler events for the given message type.
     *
     * Opens with an [TypeEntered] event. Then go the events regarding the type metadata. Then go
     * the events regarding the fields. At last, closes with an [TypeExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceMessageEvents(
        file: File,
        descriptor: Descriptor
    ) {
        val typeUrl = descriptor.file.options.getExtension(OptionsProto.typeUrlPrefix)
        val typeName = TypeName
            .newBuilder()
            .setTypeUrlPrefix(typeUrl)
            .setPackageName(file.packageName)
            .setSimpleName(descriptor.name)
            .build()
        val path = file.path
        val type = MessageType
            .newBuilder()
            .setName(typeName)
            .setFile(path)
            .build()
        yield(
            TypeEntered
                .newBuilder()
                .setFile(path)
                .setType(type)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            TypeOptionDiscovered
                .newBuilder()
                .setFile(path)
                .setType(typeName)
                .setOption(it)
                .build()
        }

        descriptor.realOneofs.forEach { produceOneofEvents(type, it) }

        descriptor.fields
            .filter { it.realContainingOneof == null }
            .forEach { produceFieldEvents(type, it) }

        yield(
            TypeExited
                .newBuilder()
                .setFile(path)
                .setType(typeName)
                .build()
        )
    }

    /**
     * Yields compiler events for the given `oneof` group.
     *
     * Opens with an [OneofGroupEntered] event. Then go the events regarding the group metadata.
     * Then go the events regarding the fields. At last, closes with an [OneofGroupExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceOneofEvents(
        type: MessageType,
        descriptor: OneofDescriptor
    ) {
        val oneofName = descriptor.name()
        val oneofGroup = OneofGroup
            .newBuilder()
            .setName(oneofName)
            .build()
        yield(
            OneofGroupEntered
                .newBuilder()
                .setFile(type.file)
                .setType(type.name)
                .setGroup(oneofGroup)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            OneofOptionDiscovered
                .newBuilder()
                .setFile(type.file)
                .setType(type.name)
                .setGroup(oneofName)
                .setOption(it)
                .build()
        }
        descriptor.fields.forEach { produceFieldEvents(type, it) }
        yield(
            OneofGroupExited
                .newBuilder()
                .setFile(type.file)
                .setType(type.name)
                .setGroup(oneofName)
                .build()
        )
    }

    /**
     * Yields compiler events for the given field.
     *
     * Opens with an [FieldEntered] event. Then go the events regarding the field options. At last,
     * closes with an [FieldExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceFieldEvents(
        type: MessageType,
        descriptor: FieldDescriptor
    ) {
        val fieldName = descriptor.name()
        val field = Field
            .newBuilder()
            .setName(fieldName)
            .setDeclaringType(type)
            .setNumber(descriptor.number)
            .setOrderOfDeclaration(descriptor.index)
            .assignTypeAndCardinality(descriptor)
            .build()
        yield(
            FieldEntered
                .newBuilder()
                .setFile(type.file)
                .setType(type.name)
                .setField(field)
                .build()
        )

        produceOptionEvents(descriptor.options) {
            FieldOptionDiscovered
                .newBuilder()
                .setFile(type.file)
                .setType(type.name)
                .setField(fieldName)
                .setOption(it)
                .build()
        }

        yield(
            FieldExited
                .newBuilder()
                .setFile(type.file)
                .setType(type.name)
                .setField(fieldName)
                .build()
        )
    }

    /**
     * Yields events regarding a set of options.
     *
     * @param options
     *     the set of options, such as `FileOptions`, `FieldOptions`, etc.
     * @param ctor
     *     a function which given an option, constructs a fitting event
     */
    private suspend fun
            SequenceScope<EventMessage>.produceOptionEvents(
        options: ExtendableMessage<*>,
        ctor: (Option) -> EventMessage
    ) {
        options.allFields.forEach { (optionDescriptor, value) ->
            val option = Option
                .newBuilder()
                .setName(optionDescriptor.name)
                .setNumber(optionDescriptor.number)
                .setType(optionDescriptor.type())
                .setValue(TypeConverter.toAny(value))
                .build()
            yield(ctor(option))
        }
    }

    /**
     * Constructs a [Type] of the receiver field.
     */
    private fun FieldDescriptor.type(): Type {
        return when (type) {
            ENUM -> enum(this)
            MESSAGE -> message(this)
            GROUP -> throw IllegalStateException(
                "Cannot process field $fullName of type $type."
            )
            else -> primitiveType().asType()
        }
    }

    private fun FieldDescriptor.primitiveType(): PrimitiveType =
        when (type) {
            BOOL -> TYPE_BOOL
            BYTES -> TYPE_BYTES
            DOUBLE -> TYPE_DOUBLE
            FIXED32 -> TYPE_FIXED32
            FIXED64 -> TYPE_FIXED64
            FLOAT -> TYPE_FLOAT
            INT32 -> TYPE_INT32
            INT64 -> TYPE_INT64
            SFIXED32 -> TYPE_SFIXED32
            SFIXED64 -> TYPE_SFIXED64
            SINT32 -> TYPE_SINT32
            SINT64 -> TYPE_SINT64
            STRING -> TYPE_STRING
            UINT32 -> TYPE_UINT32
            UINT64 -> TYPE_UINT64
            else -> throw IllegalArgumentException("`$type` is not a primitive type.")
        }

    private fun enum(field: FieldDescriptor): Type {
        val enumType = field.enumType
        val typeName = TypeName
            .newBuilder()
            .setSimpleName(enumType.name)
            .setPackageName(enumType.file.`package`)
            .setTypeUrlPrefix(enumType.file.options.getExtension(OptionsProto.typeUrlPrefix))
            .build()
        val enum = EnumType
            .newBuilder()
            .setName(typeName)
            .build()
        return Type.newBuilder()
            .setEnumeration(enum)
            .build()
    }

    private fun message(field: FieldDescriptor): Type {
        val messageType = field.messageType
        val typeName = TypeName
            .newBuilder()
            .setSimpleName(messageType.name)
            .setPackageName(messageType.file.`package`)
            .setTypeUrlPrefix(messageType.file.options.getExtension(OptionsProto.typeUrlPrefix))
            .build()
        val message = MessageType
            .newBuilder()
            .setName(typeName)
            .build()
        return Type.newBuilder()
            .setMessage(message)
            .build()
    }

    private fun Syntax.toSyntaxVersion(): SyntaxVersion =
        when (this) {
            Syntax.PROTO2 -> SyntaxVersion.PROTO2
            Syntax.PROTO3 -> SyntaxVersion.PROTO3
            Syntax.UNKNOWN -> SyntaxVersion.UNRECOGNIZED
        }

    /**
     * Assigns the field type and cardinality (`map`/`list`/`oneof_name`/`single`) to the receiver
     * builder.
     *
     * @return the receiver for method chaining
     */
    private fun Field.Builder.assignTypeAndCardinality(desc: FieldDescriptor): Field.Builder {
        if (desc.isMapField) {
            val (keyField, valueField) = desc.messageType.fields
            map = Field.OfMap
                .newBuilder()
                .setKeyType(keyField.primitiveType())
                .build()
            type = valueField.type()
        } else {
            type = desc.type()
            when {
                desc.isRepeated -> list = Empty.getDefaultInstance()
                desc.realContainingOneof != null -> oneofName = desc.realContainingOneof.name()
                else -> single = Empty.getDefaultInstance()
            }
        }
        return this
    }
}
