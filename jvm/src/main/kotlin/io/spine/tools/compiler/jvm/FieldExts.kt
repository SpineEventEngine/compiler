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

@file:JvmName("Fields")

package io.spine.tools.compiler.jvm

import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.PrimitiveType
import io.spine.tools.compiler.ast.Type
import io.spine.tools.compiler.ast.isList
import io.spine.tools.compiler.ast.isMap
import io.spine.tools.compiler.ast.simpleName
import io.spine.tools.compiler.ast.toType
import io.spine.tools.compiler.type.TypeSystem

/**
 * Obtains the name of the primary setter method for this field.
 */
public val Field.primarySetterName: String
    get() = FieldMethods(this).primarySetter

/**
 * Obtains the name of the accessor method for this field.
 */
public val Field.getterName: String
    get() = FieldMethods(this).getter

/**
 * Tells if the type of this field corresponds to a primitive type in Java.
 */
public val Field.isJavaPrimitive: Boolean
    get() = if (!type.isPrimitive) {
        false
    } else when (type.primitive) {
        PrimitiveType.TYPE_STRING, PrimitiveType.TYPE_BYTES -> false
        else -> true
    }

/**
 * Obtains the Java type of the field in the context of the given [TypeSystem].
 *
 * The returned type may have generic parameters, if the field is `repeated` or a `map`.
 *
 * @param typeSystem The type system to use for resolving the Java type.
 * @return the fully qualified reference to the Java type of the field.
 * @throws IllegalStateException If the field type cannot be converted to a Java counterpart.
 */
public fun Field.javaType(typeSystem: TypeSystem): String = when {
    isMap -> typeSystem.mapType(type.map.keyType, type.map.valueType)
    isList -> typeSystem.listOf(type.list)
    else -> type.toType().javaType(typeSystem)
}

private fun TypeSystem.mapType(key: PrimitiveType, value: Type): String {
    val keyType = key.primitiveClass()
    val valueType = value.javaType(this)
    return "${java.util.Map::class.java.canonicalName}<$keyType, $valueType>"
}

private fun TypeSystem.listOf(element: Type): String {
    val javaType = element.javaType(this)
    return "${java.util.List::class.java.canonicalName}<$javaType>"
}

/**
 * Obtains a reference the type of this field in the context of the given [message] class.
 *
 * @param message The name of the class in which the field is going to be accessed.
 * @param typeSystem The type system to resolve the Java type of the field.
 * @return a simple class name if:
 *
 *  1. The field type is either a message or an enum, and
 *  2. The type belongs to the same package as the message class.
 *
 * Otherwise, a fully qualified name is returned.
 */
@Suppress("ReturnCount")
public fun Field.typeReference(message: ClassName, typeSystem: TypeSystem): String {
    val qualifiedName = javaType(typeSystem)
    val type = type.toType()
    if (isMap || isList || type.isPrimitive) {
        return qualifiedName
    }
    // Let's see if we can refer to the field type using its simple name.
    val simpleName = type.simpleName
    val statePackage = message.packageName
    val samePackage = qualifiedName == "$statePackage.$simpleName"
    if (samePackage) {
        return simpleName
    }
    val nested = qualifiedName == "${message.canonical}.$simpleName"
    if (nested) {
        return simpleName
    }
    return qualifiedName
}
