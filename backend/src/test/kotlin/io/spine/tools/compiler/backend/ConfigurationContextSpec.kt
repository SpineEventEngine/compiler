/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.compiler.backend

import com.google.common.collect.ImmutableSet
import io.kotest.matchers.collections.shouldContainExactly
import io.spine.base.EventMessage
import io.spine.server.Application
import io.spine.server.BoundedContextBuilder
import io.spine.server.dispatch.DispatchOutcome
import io.spine.server.dispatch.DispatchOutcomes.successfulOutcome
import io.spine.server.event.EventDispatcher
import io.spine.server.type.EventClass
import io.spine.server.type.EventEnvelope
import io.spine.testing.server.testApplication
import io.spine.tools.compiler.ast.file
import io.spine.tools.compiler.settings.event.SettingsFileDiscovered
import io.spine.tools.compiler.settings.event.settingsFileDiscovered
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ConfigurationContext` should")
class ConfigurationContextSpec {

    private lateinit var application: Application
    private lateinit var subscriber: RecordingDispatcher
    private lateinit var configurationContext: ConfigurationContext

    @BeforeEach
    fun prepareReceiverContext() {
        subscriber = RecordingDispatcher()
        configurationContext = ConfigurationContext(Pipeline.generateId())
        application = testApplication {
            add(BoundedContextBuilder.assumingTests().addEventDispatcher(subscriber))
            add(configurationContext.thirdParty)
        }
    }

    @AfterEach
    fun closeApplication() {
        application.close()
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
        configurationContext.emitted(event)
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
