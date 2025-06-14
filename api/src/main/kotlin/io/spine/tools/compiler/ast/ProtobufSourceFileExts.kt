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

package io.spine.tools.compiler.ast

/**
 * Obtains a collection of message types from this source file paired with the file header.
 */
public fun ProtobufSourceFile.messages(): Collection<MessageInFile> =
    typeMap.values.map {
        messageInFile {
            message = it
            fileHeader = header
        }
    }

/**
 * Finds a message type declared in this Protobuf file.
 *
 * @param simpleName The simple name of the type.
 * @param nestedNames If the type with [simpleName] is nested, the list types enclosing it,
 *  starting from the outermost.
 * @return The message type or `null`, if there is no such declaration.
 * @see TypeName.getNestingTypeNameList
 */
public fun ProtobufSourceFile.findMessage(
    simpleName: String,
    vararg nestedNames: String
): MessageType? =
    typeMap.values.find {
        val simpleNamesMatch = (it.name.simpleName == simpleName)
        if (nestedNames.isEmpty()) {
            it.isTopLevel && simpleNamesMatch
        } else {
            simpleNamesMatch && it.name.nestingTypeNameList.toList() == nestedNames.toList()
        }
    }

/**
 * Obtains a collection of enum types from this source file paired with the file header.
 */
public fun ProtobufSourceFile.enums(): Collection<EnumInFile> =
    enumTypeMap.values.map {
        enumInFile {
            enum = it
            fileHeader = header
        }
    }

/**
 * Obtains a collection of service declarations from this source file paired with the file header.
 */
public fun ProtobufSourceFile.services(): Collection<ServiceInFile> =
    serviceMap.values.map {
        serviceInFile {
            service = it
            fileHeader = header
        }
    }
