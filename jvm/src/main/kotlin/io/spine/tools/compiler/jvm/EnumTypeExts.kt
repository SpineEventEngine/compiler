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

@file:JvmName("EnumTypes")

package io.spine.tools.compiler.jvm

import io.spine.tools.compiler.ast.EnumType
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.type.TypeSystem
import io.spine.string.simply

/**
 * Obtains the full name of the Java enum, generated from this Protobuf enum.
 *
 * @return name of the enum class generated from this enum.
 */
public fun EnumType.javaClassName(accordingTo: ProtoFileHeader): ClassName =
    name.javaClassName(accordingTo)

/**
 * Obtains a class name for the Java code generated for this enum type.
 *
 * @param typeSystem The type system to be used for obtaining the header for the proto
 *   file in which this enum type is declared.
 */
public fun EnumType.javaClassName(typeSystem: TypeSystem): ClassName {
    val header = typeSystem.findEnum(name)?.second
        ?: error("Cannot find `${simply<EnumType>()}` for the name `${name.qualifiedName}`.")
    val className = javaClassName(header)
    return className
}
