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

package io.spine.tools.compiler.plugin

import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.tools.compiler.settings.LoadsSettings
import io.spine.tools.compiler.type.TypeSystem
import io.spine.server.event.NoReaction
import io.spine.server.event.Policy
import io.spine.server.event.asB
import io.spine.server.query.QueryingClient
import io.spine.server.tuple.EitherOf2

/**
 * A policy converts one event into zero to many other events.
 *
 * As a rule of thumb, a policy should read:
 * ```markdown
 * Whenever <something happens>, then <something else must happen>.
 * ```
 * For example:
 * ```markdown
 * Whenever a field option is discovered, a validation rule must be added.
 * ```
 * To implement the policy, declare a method which reacts to an event with an event:
 * ```kotlin
 * class MyPolicy : Policy<FieldOptionDiscovered>() {
 *
 *     @React
 *     override fun whenever(@External event: FieldOptionDiscovered): Just<ValidationRuleAdded> {
 *         // Produce the event.
 *     }
 * }
 * ```
 *
 * Please note that when reacting on Protobuf Compiler events, one should mark them as
 * [@External][io.spine.core.External]. See the whole list of Protobuf compiler events
 * in `spine/compiler/events.proto`.
 *
 * One policy only accepts one kind of events. Declaring multiple methods with
 * the [@React][io.spine.server.event.React] annotation causes a runtime error.
 *
 * The `whenever` method accepts a single event and produces an `Iterable` of events. In case if
 * you need to return a single event, use [Just][io.spine.server.event.Just].
 *
 * If there are a few events, see the descendants of [Tuple][io.spine.server.tuple.Tuple].
 *
 * If there can be a few alternative events, see the descendants of
 * [Either][io.spine.server.tuple.Either].
 *
 * In case if one of the options does nothing at all, please use [io.spine.server.event.NoReaction]
 * as one of the event types.
 *
 * Finally, if there are multiple events of the same type, use a typed list,
 * e.g. `List<SomethingHappened>`.
 *
 * ### The note on avoiding intermediate types
 * Often when talking about policies, people imply converting an event into a command, not
 * an event. We believe such an approach would introduce additional complexity without adding
 * any value. Not so many commands will do anything but produce events with the same
 * information in the code generation domain. Thus, we directly convert between events.
 */
public abstract class Policy<E : EventMessage> : Policy<E>(), LoadsSettings {

    /**
     * The backing field for the [typeSystem] property.
     */
    private lateinit var _typeSystem: TypeSystem

    /**
     * The type system for resolving type information for generating events.
     *
     * A non-null value is available in
     * a [rendering pipeline][io.spine.tools.compiler.backend.Pipeline.invoke].
     */
    protected open val typeSystem: TypeSystem by lazy {
        check(::_typeSystem.isInitialized) {
            "Access to `Policy.typeSystem` property is not allowed until" +
                    " the `Code Generation` context has been injected and" +
                    " `Policy.use(typeSystem: TypeSystem)` invoked."
        }
        _typeSystem
    }

    /**
     * Assigns the type system to the policy.
     */
    internal fun use(typeSystem: TypeSystem) {
        _typeSystem = typeSystem
    }

    final override fun <P : EntityState<*>> select(type: Class<P>): QueryingClient<P> {
        return QueryingClient(context, type, javaClass.name)
    }

    final override fun <T: Any> loadSettings(cls: Class<T>): T = super.loadSettings(cls)

    final override fun settingsAvailable(): Boolean = super.settingsAvailable()

    protected companion object {

        /**
         * Creates an instance of [EitherOf2] with [NoReaction] in the `B` slot.
         */
        @JvmStatic
        public fun <E : EventMessage> ignore(): EitherOf2<E, NoReaction> {
            return NoReaction.getDefaultInstance().asB()
        }
    }
}
