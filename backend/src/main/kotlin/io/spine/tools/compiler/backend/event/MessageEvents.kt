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

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import io.spine.base.EventMessage
import io.spine.tools.compiler.ast.Documentation
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.event.FieldEntered
import io.spine.tools.compiler.ast.event.FieldExited
import io.spine.tools.compiler.ast.event.OneofGroupEntered
import io.spine.tools.compiler.ast.event.OneofGroupExited
import io.spine.tools.compiler.ast.event.fieldEntered
import io.spine.tools.compiler.ast.event.fieldExited
import io.spine.tools.compiler.ast.event.fieldOptionDiscovered
import io.spine.tools.compiler.ast.event.oneofGroupEntered
import io.spine.tools.compiler.ast.event.oneofGroupExited
import io.spine.tools.compiler.ast.event.oneofOptionDiscovered
import io.spine.tools.compiler.ast.event.typeDiscovered
import io.spine.tools.compiler.ast.event.typeEntered
import io.spine.tools.compiler.ast.event.typeExited
import io.spine.tools.compiler.ast.event.messageOptionDiscovered
import io.spine.tools.compiler.ast.oneofGroup
import io.spine.tools.compiler.ast.produceOptionEvents
import io.spine.tools.compiler.ast.withAbsoluteFile
import io.spine.tools.compiler.protobuf.name
import io.spine.tools.compiler.protobuf.realNestedTypes
import io.spine.tools.compiler.protobuf.toField
import io.spine.tools.compiler.protobuf.toMessageType

/**
 * Produces events for a message.
 */
internal class MessageEvents(header: ProtoFileHeader) : DeclarationEvents<Descriptor>(header) {

    /**
     * Yields events for the given message type.
     *
     * Starts with [TypeDiscovered][io.spine.tools.compiler.ast.event.TypeDiscovered] and
     * [io.spine.tools.compiler.ast.event.TypeEntered] events.
     * Then the events regarding the type metadata come.
     * Then go the events regarding the fields.
     * At last, closes with an [TypeExited][io.spine.tools.compiler.ast.event.TypeExited] event.
     *
     * @param desc The descriptor of the message type.
     */
    override suspend fun SequenceScope<EventMessage>.produceEvents(
        desc: Descriptor
    ) {
        val typeName = desc.name()
        val path = header.file
        val messageType = desc.toMessageType().withAbsoluteFile(path)
        yield(
            typeDiscovered {
                file = path
                type = messageType
            }
        )
        yield(
            typeEntered {
                file = path
                type = messageType.name
            }
        )
        produceOptionEvents(desc.options, desc) {
            messageOptionDiscovered {
                file = path
                subject = messageType
                option = it
            }
        }

        desc.realOneofs.forEach {
            produceOneofEvents(it)
        }

        desc.fields
            .filter { it.realContainingOneof == null }
            .forEach { produceFieldEvents(it) }

        desc.realNestedTypes().forEach {
            produceEvents(desc = it)
        }

        // Do not filter out nested enum types either.
        val enums = EnumEvents(header)
        desc.enumTypes.forEach {
            enums.apply {
                produceEvents(desc = it)
            }
        }

        yield(
            typeExited {
                file = path
                type = typeName
            }
        )
    }

    /**
     * Yields compiler events for the given `oneof` group.
     *
     * Opens with an [OneofGroupEntered] event.
     * Then go the events regarding the group metadata.
     * Then go the events regarding the fields.
     * At last, closes with an [OneofGroupExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceOneofEvents(
        desc: OneofDescriptor
    ) {
        val typeName = desc.containingType.name()
        val documentation = Documentation.of(desc.containingType.file)
        val oneofName = desc.name()
        val oneofGroup = oneofGroup {
            name = oneofName
            declaringType = typeName
            doc = documentation.forOneof(desc)
        }
        val path = header.file
        yield(
            oneofGroupEntered {
                file = path
                type = typeName
                group = oneofGroup
            }
        )
        produceOptionEvents(desc.options, desc) {
            oneofOptionDiscovered {
                file = path
                subject = oneofGroup
                option = it
            }
        }
        desc.fields.forEach {
            produceFieldEvents(it)
        }
        yield(
            oneofGroupExited {
                file = path
                type = typeName
                group = oneofName
            }
        )
    }

    /**
     * Yields compiler events for the given field.
     *
     * Opens with an [FieldEntered] event.
     * Then events regarding the field options are emitted.
     * At last, closes with an [FieldExited] event.
     */
    @Suppress("DEPRECATION") /* Populate deprecated fields in `FieldOptionDiscovered`
        for backward compatibility. */
    private suspend fun SequenceScope<EventMessage>.produceFieldEvents(
        desc: FieldDescriptor
    ) {
        val typeName = desc.containingType.name()
        val fieldName = desc.name()
        val theField = desc.toField()
        val path = header.file
        yield(
            fieldEntered {
                file = path
                type = typeName
                field = theField
            }
        )
        produceOptionEvents(desc.options, desc) {
            fieldOptionDiscovered {
                file = path
                type = typeName
                field = fieldName
                subject = theField
                option = it
            }
        }
        yield(
            fieldExited {
                file = path
                type = typeName
                field = fieldName
            }
        )
    }
}
