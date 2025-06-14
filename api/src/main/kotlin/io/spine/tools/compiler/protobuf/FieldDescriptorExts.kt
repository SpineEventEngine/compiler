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
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.FieldName
import io.spine.tools.compiler.ast.FieldType
import io.spine.tools.compiler.ast.MapEntryType
import io.spine.tools.compiler.ast.PrimitiveType
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BOOL
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BYTES
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_FIXED32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_FIXED64
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_FLOAT
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_INT32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_INT64
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_SFIXED32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_SFIXED64
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_SINT32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_SINT64
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_UINT32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_UINT64
import io.spine.tools.compiler.ast.Type
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.coordinates
import io.spine.tools.compiler.ast.documentation
import io.spine.tools.compiler.ast.field
import io.spine.tools.compiler.ast.fieldName
import io.spine.tools.compiler.ast.fieldType
import io.spine.tools.compiler.ast.mapEntryType
import io.spine.tools.compiler.ast.options
import io.spine.tools.compiler.ast.toType
import io.spine.tools.compiler.ast.type
import kotlin.reflect.KClass

/**
 * Obtains the name of this field as a [FieldName].
 */
public fun FieldDescriptor.name(): FieldName = fieldName { value = name }

/**
 * Converts this field descriptor into a [Field].
 */
public fun FieldDescriptor.toField(): Field =
    field {
        val messageType = containingType
        val declaredIn = messageType.name()
        val descriptor = this@toField
        name = name()
        // New `FieldType` and `group_name` API.
        type = toFieldType()
        declaringType = declaredIn
        number = descriptor.number
        orderOfDeclaration = index
        realContainingOneof?.let {
            enclosingOneof = it.name()
        }
        doc = messageType.documentation().forField(descriptor)
        span = messageType.coordinates().forField(descriptor)
        option.addAll(options())
    }

/**
 * Constructs a [Type] of the receiver field.
 */
public fun FieldDescriptor.type(): Type {
    return when (type) {
        ENUM -> enum(this)
        MESSAGE -> message(this)
        GROUP -> cannotConvertTo(Type::class)
        else -> primitiveType().toType()
    }
}

/**
 * Obtains the type of the given [field] as a message type.
 */
private fun message(field: FieldDescriptor): Type = type {
    message = field.messageType.name()
}

/**
 * Converts this field type into an instance of [PrimitiveType], or
 * `null` if the type is not primitive
 */
@Suppress("CyclomaticComplexMethod")
public fun FieldDescriptor.Type.toPrimitiveType(): PrimitiveType? = when (this) {
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
    else -> null
}

/**
 * Obtains the type of this field as a [PrimitiveType] or throws an exception
 * if the type is not primitive.
 */
public fun FieldDescriptor.primitiveType(): PrimitiveType {
    val result = type.toPrimitiveType()
    check(result != null) {
        "Unable to convert the type `$this` to `PrimitiveType`."
    }
    return result
}

private fun FieldDescriptor.cannotConvertTo(destination: KClass<*>): Nothing = error(
    "Cannot convert the field descriptor `$fullName`" +
            " (type: `$type`) to `${destination.simpleName}`."
)

/**
 * Obtains the type of the given [field] as an enum type.
 */
private fun enum(field: FieldDescriptor): Type = type {
    enumeration = field.enumType.name()
}

/**
 * Transforms this `Iterable` of field descriptors into an `Iterable` with [Field] instances.
 */
internal fun Iterable<FieldDescriptor>.mapped(): Iterable<Field> = map { it.toField() }

public fun FieldDescriptor.toFieldType(): FieldType = fieldType {
    when {
        isMessage -> message = toTypeName()
        isEnum -> enumeration = toTypeName()
        isPrimitive -> primitive = type.toPrimitiveType()!!
        isMap -> map = toMapEntryType()
        /* The order of checking is important here. Maps are also `isRepeated`.
           Checking after `isMap` above ensures we get the "pure" list type. */
        isRepeated /*&& !isMap*/ -> list = type()
        else -> cannotConvertTo(FieldType::class)
    }
}

/** Tells if this field is of a singular message type. */
private val FieldDescriptor.isMessage: Boolean
    get() = (type == MESSAGE) && !isRepeated

/** Tells if this field is of a singular enum type. */
private val FieldDescriptor.isEnum: Boolean
    get() = (type == ENUM) && !isRepeated

/** Tells if this field has a primitive type. */
private val FieldDescriptor.isPrimitive: Boolean
    get() = (type.toPrimitiveType() != null) && !isRepeated

/** Tells if this field is a map. */
private val FieldDescriptor.isMap: Boolean
    get() = isMapField

/** Obtains a type name for a singular message or enum field. */
private fun FieldDescriptor.toTypeName(): TypeName = when {
    isMessage -> messageType.name()
    isEnum -> enumType.name()
    else -> cannotConvertTo(TypeName::class)
}

/** Obtains an entry type for a map field. */
private fun FieldDescriptor.toMapEntryType(): MapEntryType {
    check(isMap) { "The field `$fullName` is not a map." }
    val (keyField, valueField) = messageType.fields
    return mapEntryType {
        keyType = keyField.primitiveType()
        valueType = valueField.type()
    }
}
