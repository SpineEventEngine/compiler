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

@file:JvmName("MessageTypes")

package io.spine.tools.compiler.jvm

import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.type.TypeSystem
import io.spine.string.simply
import java.nio.file.Path

/**
 * Obtains the path to the `.java` file, generated from this message.
 *
 * The class which represents this message might not be the top-level class of the Java file,
 * which is determined by the options in the given Protobuf file header.
 */
public fun MessageType.javaFile(accordingTo: ProtoFileHeader): Path =
    name.javaFile(accordingTo)

/**
 * Obtains the full name of the Java class, generated from this message.
 *
 * @return the name of the class generated from this message.
 */
public fun MessageType.javaClassName(accordingTo: ProtoFileHeader): ClassName =
    name.javaClassName(accordingTo)

/**
 * Obtains a class name for the Java code generated for this message type.
 *
 * @param typeSystem The type system to be used for obtaining the header for the proto
 *   file in which this message type is declared.
 */
public fun MessageType.javaClassName(typeSystem: TypeSystem): ClassName {
    val header = typeSystem.findMessage(name)?.second
        ?: error("Cannot find `${simply<MessageType>()}` for the name `${name.qualifiedName}`.")
    val className = javaClassName(header)
    return className
}

/**
 * Obtains the [Class] instance for this [MessageType], if any.
 *
 * The function returns a non-`null` result if a Java class denoted by this
 * [MessageType] is present on the classpath.
 *
 * @param accordingTo The proto file header, according to which the Java class
 *  name is determined.
 */
public fun MessageType.javaClass(accordingTo: ProtoFileHeader): Class<*>? {
    val name = javaClassName(accordingTo)
    return name.javaClass()
}

/**
 * Obtains the [Class] instance for this [MessageType], if any.
 *
 * The function returns a non-`null` result if a class denoted by this
 * [MessageType] is present on the classpath.
 *
 * @param typeSystem The type system to be used for obtaining the header for the proto
 *  file in which this message type is declared.
 */
public fun MessageType.javaClass(typeSystem: TypeSystem): Class<*>? {
    val name = javaClassName(typeSystem)
    return name.javaClass()
}

/**
 * Looks for the field with the given [name] in this [MessageType].
 *
 * @return the found [Field], or `null` if this [MessageType] does not have such a field.
 */
public fun MessageType.findField(name: String): Field? =
    fieldList.find { it.name.value == name }
