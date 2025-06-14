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

@file:Suppress("DEPRECATION") /* The deprecated types are still the dependencies. */

package io.spine.tools.compiler.ast

import com.google.protobuf.AnyProto
import com.google.protobuf.Duration
import com.google.protobuf.DurationProto
import com.google.protobuf.Timestamp
import com.google.protobuf.TimestampProto
import io.kotest.matchers.collections.shouldBeEmpty
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.tools.compiler.protobuf.ProtoFileList
import io.spine.tools.compiler.protobuf.toPbSourceFile
import io.spine.tools.compiler.type.TypeSystem
import io.spine.core.ActorContext
import io.spine.core.ActorContextProto
import io.spine.core.Command
import io.spine.core.CommandContext
import io.spine.core.CommandId
import io.spine.core.CommandProto
import io.spine.core.DiagnosticsProto
import io.spine.core.Enrichment
import io.spine.core.EnrichmentProto
import io.spine.core.EventContext
import io.spine.core.EventId
import io.spine.core.EventProto
import io.spine.core.MessageId
import io.spine.core.Origin
import io.spine.core.RejectionEventContext
import io.spine.core.TenantId
import io.spine.core.TenantIdProto
import io.spine.core.UserId
import io.spine.core.UserIdProto
import io.spine.core.Version
import io.spine.core.VersionProto
import io.spine.net.EmailAddress
import io.spine.net.EmailAddressProto
import io.spine.net.InternetDomain
import io.spine.net.InternetDomainProto
import io.spine.time.TimeProto
import io.spine.time.ZoneId
import io.spine.time.ZoneOffset
import io.spine.ui.LanguageProto
import java.io.File
import org.junit.jupiter.api.Test
import com.google.protobuf.Any as ProtoAny

/**
 * Tests that dependencies of [EventContext] are calculated in full.
 */
internal class EventContextDependenciesTest {

    private val typeSystem: TypeSystem by lazy {
        val descriptors = setOf(
            ActorContextProto.getDescriptor(),
            AnyProto.getDescriptor(),
            CommandProto.getDescriptor(),
            DiagnosticsProto.getDescriptor(),
            DurationProto.getDescriptor(),
            EmailAddressProto.getDescriptor(),
            EnrichmentProto.getDescriptor(),
            EventProto.getDescriptor(),
            InternetDomainProto.getDescriptor(),
            LanguageProto.getDescriptor(),
            TenantIdProto.getDescriptor(),
            TimeProto.getDescriptor(),
            TimestampProto.getDescriptor(),
            UserIdProto.getDescriptor(),
            VersionProto.getDescriptor(),
        )
        val protoSources = descriptors.map { it.toPbSourceFile() }.toSet()
        val protoFiles = descriptors.map { File(it.file.name) }
        TypeSystem(ProtoFileList(protoFiles), protoSources)
    }

    @Test
    fun `full dependencies`() {
        val eventContext = messageTypeOf<EventContext>()
        val deps =
            MessageTypeDependencies(eventContext, setOf(CARDINALITY_SINGLE), typeSystem).asSet()

        val expected = setOf(
            messageTypeOf<ActorContext>(),
            messageTypeOf<Command.SystemProperties>(),
            messageTypeOf<Command>(),
            messageTypeOf<CommandContext.Schedule>(),
            messageTypeOf<CommandContext>(),
            messageTypeOf<CommandId>(),
            messageTypeOf<Duration>(),
            messageTypeOf<EmailAddress>(),
            messageTypeOf<Enrichment.Container>(),
            messageTypeOf<Enrichment>(),
            messageTypeOf<EventContext>(),
            messageTypeOf<EventId>(),
            messageTypeOf<InternetDomain>(),
            messageTypeOf<MessageId>(),
            messageTypeOf<Origin>(),
            messageTypeOf<ProtoAny>(),
            messageTypeOf<RejectionEventContext>(),
            messageTypeOf<TenantId>(),
            messageTypeOf<Timestamp>(),
            messageTypeOf<UserId>(),
            messageTypeOf<Version>(),
            messageTypeOf<ZoneId>(),
            messageTypeOf<ZoneOffset>(),
        ).map { it.qualifiedName }.toSet()

        val dependencies = deps.map { it.qualifiedName }

        dependencies.minus(expected).shouldBeEmpty()
    }
}
