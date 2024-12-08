/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.backend.event

import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import io.spine.base.EventMessage
import io.spine.protodata.ast.ProtoFileHeader
import io.spine.protodata.ast.constantName
import io.spine.protodata.ast.event.enumConstantEntered
import io.spine.protodata.ast.event.enumConstantExited
import io.spine.protodata.ast.event.enumConstantOptionDiscovered
import io.spine.protodata.ast.event.enumDiscovered
import io.spine.protodata.ast.event.enumEntered
import io.spine.protodata.ast.event.enumExited
import io.spine.protodata.ast.event.enumOptionDiscovered
import io.spine.protodata.ast.produceOptionEvents
import io.spine.protodata.protobuf.buildConstant
import io.spine.protodata.protobuf.name
import io.spine.protodata.protobuf.toEnumType

/**
 * Produces events for an enum.
 */
internal class EnumCompilerEvents(
    private val header: ProtoFileHeader
) {

    /**
     * Yields compiler events for the given enum type.
     *
     * Opens with an [EnumEntered][io.spine.protodata.ast.event.EnumEntered] event.
     * Then the events regarding the type metadata go.
     * Then go the events regarding the enum constants.
     * At last, closes with an [EnumExited][io.spine.protodata.ast.event.EnumExited] event.
     */
    internal suspend fun SequenceScope<EventMessage>.produceEvents(
        desc: EnumDescriptor
    ) {
        val enumType = desc.toEnumType()
        val typeName = desc.name()
        val path = header.file
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
                this.type = typeName
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
     * Opens with an [EnumConstantEntered][io.spine.protodata.ast.event.EnumConstantEntered] event.
     * Then go the events regarding the constant options.
     * At last, closes with an
     * [EnumConstantExited][io.spine.protodata.ast.event.EnumConstantExited] event.
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
                type = typeName
                constant = name
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
