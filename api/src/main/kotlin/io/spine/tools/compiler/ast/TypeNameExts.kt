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

@file:JvmName("TypeNames")

package io.spine.tools.compiler.ast

import com.google.protobuf.Message
import io.spine.protobuf.defaultInstance
import io.spine.tools.compiler.protobuf.toMessageType
import io.spine.tools.compiler.type.TypeSystem
import io.spine.string.shortly
import io.spine.string.simply

/**
 * Tells if this type is `google.protobuf.Any`.
 */
public val TypeNameOrBuilder.isAny: Boolean
    get() = packageName == "google.protobuf" && simpleName == "Any"

/**
 * Obtains a fully qualified name of a `TypeName` or its builder.
 */
public val TypeNameOrBuilder.qualifiedName: String
    get() {
        val names = buildList<String> {
            add(packageName)
            addAll(nestingTypeNameList)
            add(simpleName)
        }
        return names.filter { it.isNotEmpty() }.joinToString(separator = ".")
    }

/**
 * Obtains a [MessageType] that corresponds to the message class
 * specified in the generic parameter [T].
 */
public inline fun <reified T: Message> messageTypeOf(): MessageType {
    return T::class.java.defaultInstance.descriptorForType.toMessageType()
}

/**
 * Converts this type name to an instance of [MessageType] finding it using the given [typeSystem].
 *
 * @throws IllegalStateException If the type system does not have a corresponding `MessageType`.
 */
public fun TypeName.toMessageType(typeSystem: TypeSystem): MessageType {
    val found = typeSystem.findMessage(this)?.first
    checkFound<MessageType>(found)
    return found!!
}

/**
 * Converts this type name to an instance of [EnumType] finding it using the given [typeSystem].
 *
 * @throws IllegalStateException If the type system does not have a corresponding `EnumType`.
 */
public fun TypeName.toEnumType(typeSystem: TypeSystem): EnumType {
    val found = typeSystem.findEnum(this)?.first
    checkFound<EnumType>(found)
    return found!!
}

private inline fun <reified T: Message> TypeName.checkFound(found: Message?) =
    check(found != null) {
        "Unable to find `${simply<T>()}` for the type name `${shortly()}`."
    }
