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

package io.spine.tools.compiler.backend

import com.google.common.collect.ImmutableSet
import io.kotest.matchers.collections.shouldContainExactly
import io.spine.base.EventMessage
import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder
import io.spine.server.dispatch.DispatchOutcome
import io.spine.server.dispatch.DispatchOutcomes.successfulOutcome
import io.spine.server.event.EventDispatcher
import io.spine.server.type.EventClass
import io.spine.server.type.EventEnvelope
import io.spine.tools.compiler.ast.file
import io.spine.tools.compiler.settings.event.SettingsFileDiscovered
import io.spine.tools.compiler.settings.event.settingsFileDiscovered
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ConfigurationContext` should")
class ConfigurationContextSpec {

    private lateinit var context: BoundedContext
    private lateinit var subscriber: RecordingDispatcher
    private lateinit var configurationContext: ConfigurationContext

    @BeforeEach
    fun prepareReceiverContext() {
        subscriber = RecordingDispatcher()
        context = BoundedContextBuilder.assumingTests()
            .addEventDispatcher(subscriber)
            .build()
        configurationContext = ConfigurationContext(Pipeline.generateId())
    }

    @AfterEach
    fun closeContext() {
        configurationContext.close()
        context.close()
    }

    @Test
    fun `emit file configuration event`() {
        val settingsFile = file {
            path = "foo/bar.bin"
        }
        val event = settingsFileDiscovered {
            file = settingsFile
        }
        checkEvent(event)
    }

    private fun checkEvent(event: EventMessage) {
        configurationContext.use {
            it.emitted(event)
        }
        subscriber.receivedEvents shouldContainExactly listOf(event)
    }
}

/**
 * Remembers [external events][externalEventClasses] dispatched to it.
 */
private class RecordingDispatcher : EventDispatcher {

    val receivedEvents = mutableListOf<EventMessage>()

    override fun messageClasses(): ImmutableSet<EventClass> = externalEventClasses()

    override fun dispatch(envelope: EventEnvelope): DispatchOutcome {
        receivedEvents.add(envelope.message())
        return successfulOutcome(envelope.messageId())
    }

    override fun externalEventClasses(): ImmutableSet<EventClass> = EventClass.setOf(
        SettingsFileDiscovered::class.java
    )

    override fun domesticEventClasses(): ImmutableSet<EventClass> =
        EventClass.emptySet()
}
