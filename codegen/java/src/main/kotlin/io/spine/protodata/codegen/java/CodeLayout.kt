/*
 * Copyright 2023, TeamDev. All rights reserved.
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

@file:JvmName("Ast2Java")

package io.spine.protodata.codegen.java

import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import io.spine.protodata.EnumType
import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.ServiceName
import io.spine.protodata.TypeName
import io.spine.string.camelCase
import io.spine.protodata.find
import io.spine.protodata.nameWithoutExtension
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Obtains the path to the `.java` file, generated from this message.
 *
 * The class which represents this message might not be the top level class of the Java file.
 */
public fun MessageType.javaFile(declaredIn: File): Path =
    name.javaFile(declaredIn)

internal fun TypeName.javaFile(declaredIn: File): Path {
    val packageName = declaredIn.javaPackage()
    val javaMultipleFiles = declaredIn.javaMultipleFiles()
    val topLevelClassName = when {
        !javaMultipleFiles -> declaredIn.javaOuterClassName()
        nestingTypeNameList.isNotEmpty() -> nestingTypeNameList.first()
        else -> simpleName
    }
    val packageAsPath = packageName.replace('.', java.io.File.separatorChar)
    return Path(packageAsPath, "$topLevelClassName.java")
}

/**
 * Obtains the full name of the Java class, generated from this message.
 *
 * @return name of the class generated from this message.
 */
public fun MessageType.javaClassName(declaredIn: File): ClassName =
    name.javaClassName(declaredIn)

/**
 * Obtains the full name of the Java enum, generated from this Protobuf enum.
 *
 * @return name of the enum class generated from this enum.
 */
public fun EnumType.javaClassName(declaredIn: File): ClassName =
    name.javaClassName(declaredIn)

/**
 * Obtains a Java class name for the given Protobuf declaration.
 *
 * @param declaredIn
 *        the Protobuf file where the declaration resides in.
 * @param block
 *        the block of code which adds the name elements to the class name.
 */
private fun composeJavaClassName(
    declaredIn: File,
    block: MutableList<String>.() -> Unit
): ClassName {
    val packageName = declaredIn.javaPackage()
    val javaMultipleFiles = declaredIn.javaMultipleFiles()
    val nameElements = mutableListOf<String>()
    if (!javaMultipleFiles) {
        nameElements.add(declaredIn.javaOuterClassName())
    }
    block(nameElements)
    return ClassName(packageName, nameElements)
}

/**
 * Obtains a fully-qualified Java class, generated for the Protobuf type with this name.
 */
internal fun TypeName.javaClassName(declaredIn: File): ClassName =
    composeJavaClassName(declaredIn) {
        addAll(nestingTypeNameList)
        add(simpleName)
    }

/**
 * Obtains a fully-qualified Java class, generated for the gRPC service with this name.
 */
internal fun ServiceName.javaClassName(declaredIn: File): ClassName =
    composeJavaClassName(declaredIn) {
        add(simpleName + "Grpc")
    }

/**
 * Obtains a name of a Java package for the code generated from this Protobuf file.
 *
 * @return A value of the `java_package` option, if it is set.
 *         Otherwise, returns the package name of the file.
 */
public fun File.javaPackage(): String =
    optionList.find("java_package", StringValue::class.java)
        ?.value
        ?: packageName

/**
 * Obtains a value of `java_multiple_files` option set for this file.
 */
public fun File.javaMultipleFiles(): Boolean =
    optionList.find("java_multiple_files", BoolValue::class.java)
        ?.value
        ?: false

/**
 * Obtains a name of the Java outer class generated for this Protobuf file.
 *
 * @return A value of `java_outer_classname` option, if it set for this file.
 */
public fun File.javaOuterClassName(): String =
    optionList.find("java_outer_classname", StringValue::class.java)
        ?.value
        ?: nameWithoutExtension().camelCase()

