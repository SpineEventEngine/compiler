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

@file:JvmName("FieldTypes")

package io.spine.tools.compiler.ast

import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_LIST
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_MAP
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.tools.compiler.ast.FieldType.KindCase.ENUMERATION
import io.spine.tools.compiler.ast.FieldType.KindCase.LIST
import io.spine.tools.compiler.ast.FieldType.KindCase.MAP
import io.spine.tools.compiler.ast.FieldType.KindCase.MESSAGE
import io.spine.tools.compiler.ast.FieldType.KindCase.PRIMITIVE
import io.spine.tools.compiler.type.TypeSystem
import io.spine.string.shortly
import io.spine.string.simply

/**
 * Obtains a human-readable name of this field type.
 */
public val FieldType.name: String
    get() = when (kindCase) {
        MESSAGE -> message.qualifiedName
        ENUMERATION -> enumeration.qualifiedName
        PRIMITIVE -> primitive.protoName
        LIST -> "repeated ${list.name}"
        MAP -> "map<${map.keyType.protoName}, ${map.valueType.name}>"
        else -> kindCase.name
    }

/**
 * Indicates if this field is `repeated`, but not a `map`.
 *
 * @see isSingular
 * @see isMap
 */
public val FieldType.isList: Boolean
    get() = hasList()

/**
 * Indicates if this field type represents a message.
 *
 * @see isSingular
 * @see isList
 */
public val FieldType.isMap: Boolean
    get() = hasMap()

/**
 * Indicates if this type holds one value such as primitive, message, or enum item.
 *
 * @see isList
 * @see isMap
 */
public val FieldType.isSingular: Boolean
    get() = isMessage || isEnum || isPrimitive

/**
 * Obtains a cardinality of this field type.
 *
 * @see Cardinality
 */
public val FieldType.cardinality: Cardinality
    get() = when (kindCase) {
        MESSAGE,
        ENUMERATION,
        PRIMITIVE -> CARDINALITY_SINGLE
        LIST -> CARDINALITY_LIST
        MAP -> CARDINALITY_MAP
        else -> error("Unable to convert `$kindCase` to `Cardinality`.")
    }

/**
 * Converts this field type to [Type].
 *
 * @throws IllegalStateException If this is field type is a list or a map.
 *
 * @see extractType
 */
public fun FieldType.toType(): Type = type {
    val self = this@toType
    val dsl = this@type
    when {
        isMessage -> dsl.message = self.message
        isEnum -> dsl.enumeration = self.enumeration
        isPrimitive -> dsl.primitive = self.primitive
        else -> error("Cannot convert ${self.shortly()} to `${simply<Type>()}`.")
    }
}

/**
 * Obtains full message type information for singular fields, lists, or maps storing messages.
 *
 * @param typeSystem The type system to be used for obtaining type information.
 *
 * @return the message type instance or `null` if this field type is not a message,
 *   or if it does not refer to message being a list or a map.
 *
 * @see extractType
 * @see extractPrimitiveType
 */
public fun FieldType.extractMessageType(typeSystem: TypeSystem): MessageType? = when {
    isMessage -> message.toMessageType(typeSystem)
    isList -> list.maybeMessageType(typeSystem)
    isMap -> map.valueType.maybeMessageType(typeSystem)
    else -> null
}

/**
 * Optionally converts this type into [MessageType] if this type is a message.
 */
private fun Type.maybeMessageType(typeSystem: TypeSystem): MessageType? =
    if (isMessage) toMessageType(typeSystem) else null

/**
 * Obtains a name of the field type if it is a message or an enum.
 *
 * @return the name of the message or enum type, or `null` otherwise.
 *
 * @see extractType
 */
public fun FieldType.extractTypeName(): TypeName? = when {
    isMessage -> message
    isEnum -> enumeration
    isList -> list.maybeTypeName()
    isMap -> map.valueType.maybeTypeName()
    else -> null
}

/**
 * Obtains the name of this type for enums and messages, returning `null` otherwise.
 */
private fun Type.maybeTypeName(): TypeName? = when {
    isMessage -> message
    isEnum -> enumeration
    else -> null
}

/**
 * Obtains the primitive type this field type refers to directly,
 * or via the type of list or map values.
 *
 * @return the name of the message or enum type, or `null` otherwise.
 */
public fun FieldType.extractPrimitiveType(): PrimitiveType? = when {
    isPrimitive -> primitive
    isList -> list.maybePrimitiveType()
    isMap -> map.valueType.maybePrimitiveType()
    else -> null
}

/**
 * Obtains the value of the [PrimitiveType] this type is such, or `null` otherwise.
 */
private fun Type.maybePrimitiveType(): PrimitiveType? =
    if (isPrimitive) primitive else null

/**
 * Extracts the information about the direct type or the type of elements from this field type.
 *
 * @see extractTypeName
 * @see extractMessageType
 */
public fun FieldType.extractType(): Type = when {
    isMessage -> toType()
    isEnum -> toType()
    isPrimitive -> toType()
    isMap -> map.valueType
    isList -> list
    else -> error("Cannot get type info from the field type `${this.shortly()}`.")
}

/**
 * Indicates if this field type is a message, or it refers to a message type being
 * a list or a map with such.
 *
 * @see refersToAny
 */
public fun FieldType.refersToMessage(): Boolean = when {
    isMessage -> true
    isMap -> map.valueType.isMessage
    isList -> list.isMessage
    else -> false
}

/**
 * Indicates if this field type refers to [com.google.protobuf.Any].
 *
 * @see refersToMessage
 */
public fun FieldType.refersToAny(): Boolean = when {
    isAny -> true
    isMap -> map.valueType.isAny
    isList -> list.isAny
    else -> false
}
