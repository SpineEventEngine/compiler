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

package io.spine.tools.compiler.backend.event

import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import io.spine.base.EventMessage
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.constantName
import io.spine.tools.compiler.ast.event.enumConstantEntered
import io.spine.tools.compiler.ast.event.enumConstantExited
import io.spine.tools.compiler.ast.event.enumConstantOptionDiscovered
import io.spine.tools.compiler.ast.event.enumDiscovered
import io.spine.tools.compiler.ast.event.enumEntered
import io.spine.tools.compiler.ast.event.enumExited
import io.spine.tools.compiler.ast.event.enumOptionDiscovered
import io.spine.tools.compiler.ast.produceOptionEvents
import io.spine.tools.compiler.ast.withAbsoluteFile
import io.spine.tools.compiler.protobuf.buildConstant
import io.spine.tools.compiler.protobuf.name
import io.spine.tools.compiler.protobuf.toEnumType

/**
 * Produces events for an enum.
 */
internal class EnumEvents(header: ProtoFileHeader) : DeclarationEvents<EnumDescriptor>(header) {

    /**
     * Yields events for the given enum type.
     *
     * Opens with an [EnumEntered][io.spine.tools.compiler.ast.event.EnumEntered] event.
     * Then the events regarding the type metadata go.
     * Then go the events regarding the enum constants.
     * At last, closes with an [EnumExited][io.spine.tools.compiler.ast.event.EnumExited] event.
     */
    override suspend fun SequenceScope<EventMessage>.produceEvents(desc: EnumDescriptor) {
        val path = header.file
        val enumType = desc.toEnumType().withAbsoluteFile(path)
        val typeName = desc.name()
        yield(
            enumDiscovered {
                file = path
                type = enumType
            }
        )
        yield(
            enumEntered {
                file = path
                type = typeName
            }
        )
        produceOptionEvents(desc.options, desc) {
            enumOptionDiscovered {
                file = path
                subject = enumType
                option = it
            }
        }
        desc.values.forEach {
            produceConstantEvents(it)
        }
        yield(
            enumExited {
                file = path
                this.type = typeName
            }
        )
    }

    /**
     * Yields compiler events for the given enum constant.
     *
     * Opens with an [EnumConstantEntered][io.spine.tools.compiler.ast.event.EnumConstantEntered] event.
     * Then go the events regarding the constant options.
     * At last, closes with an
     * [EnumConstantExited][io.spine.tools.compiler.ast.event.EnumConstantExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceConstantEvents(
        desc: EnumValueDescriptor
    ) {
        val typeName = desc.type.name()
        val name = constantName {
            value = desc.name
        }
        val theConstant = buildConstant(desc, typeName)
        val path = header.file
        yield(
            enumConstantEntered {
                file = path
                type = typeName
                constant = theConstant
            }
        )
        produceOptionEvents(desc.options, desc) {
            enumConstantOptionDiscovered {
                file = path
                subject = theConstant
                option = it
            }
        }
        yield(
            enumConstantExited {
                file = path
                type = typeName
                constant = name
            }
        )
    }
}
