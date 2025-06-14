
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

import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import io.spine.tools.compiler.ast.EnumConstant
import io.spine.tools.compiler.ast.EnumType
import io.spine.tools.compiler.ast.Type
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.constantName
import io.spine.tools.compiler.ast.coordinates
import io.spine.tools.compiler.ast.copy
import io.spine.tools.compiler.ast.documentation
import io.spine.tools.compiler.ast.enumConstant
import io.spine.tools.compiler.ast.enumType
import io.spine.tools.compiler.ast.options
import io.spine.tools.compiler.ast.type

/**
 * Obtains the name of this enum type as a [TypeName].
 */
public fun EnumDescriptor.name(): TypeName = buildTypeName(name, file, containingType)

/**
 * Converts this enum descriptor into [EnumType] instance.
 *
 * @see EnumDescriptor.toType
 */
public fun EnumDescriptor.toEnumType(): EnumType =
    enumType {
        val self = this@toEnumType
        val typeName = name()
        name = typeName
        option.addAll(options())
        file = getFile().file()
        constant.addAll(values.map { it.toEnumConstant(typeName) })
        if (containingType != null) {
            declaredIn = containingType.name()
        }
        doc = documentation().forEnum(self)
        span = coordinates().forEnum(self)
    }

/**
 * Converts this enum descriptor into an instance of [Type].
 *
 * @see EnumDescriptor.toEnumType
 */
public fun EnumDescriptor.toType(): Type = type {
    enumeration = name()
}

/**
 * Converts this enum value descriptor into an [EnumConstant] with options.
 *
 * @see buildConstant
 */
public fun EnumValueDescriptor.toEnumConstant(declaringType: TypeName): EnumConstant {
    val self = this
    val constant = buildConstant(self, declaringType)
    return constant.copy {
        option.addAll(options())
    }
}

/**
 * Converts this enum value descriptor into an [EnumConstant].
 *
 * The resulting [EnumConstant] will not reflect the options on the enum constant.
 *
 * @see toEnumConstant
 */
public fun buildConstant(desc: EnumValueDescriptor, declaringType: TypeName): EnumConstant =
    enumConstant {
        name = constantName { value = desc.name }
        declaredIn = declaringType
        number = desc.number
        orderOfDeclaration = desc.index
        val enumType = desc.type
        doc = enumType.documentation().forEnumConstant(desc)
        span = enumType.coordinates().forEnumConstant(desc)
    }
