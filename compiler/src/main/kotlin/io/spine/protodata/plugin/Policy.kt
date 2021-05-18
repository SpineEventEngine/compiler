/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.protodata.plugin

import com.google.common.collect.ImmutableSet
import com.google.common.flogger.LazyArgs.lazy
import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.logging.Logging
import io.spine.protodata.QueryingClient
import io.spine.server.BoundedContext
import io.spine.server.event.AbstractEventReactor
import io.spine.server.type.EventClass

/**
 * A policy converts one event into zero to many other events.
 *
 * As a rule of thumb, a policy should read:
 * Whenever <something happens>, then <something else must happen>.
 *
 * For example:
 * Whenever a field option is discovered, a validation rule must be added.
 *
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
 * in `spine/protodata/events.proto`.
 *
 * One policy only accepts one kind of events. Declaring multiple methods with
 * the [@React][io.spine.server.event.React] annotation causes a runtime error.
 *
 * The `whenever` method accepts a single event and produces an `Iterable` of events. In case if
 * you need to return a single event, use [Just].
 * If there are a few events, see the descendants of [io.spine.server.tuple.Tuple].
 * If there can be a few alternative events, see the  descendants of [io.spine.server.tuple.Either].
 * In case if one of the options doing nothing at all, use [io.spine.server.model.Nothing] as one
 * of the event types.
 * Finally, if there are multiple events of the same type, use a typed list,
 * e.g. `List<SomethingHappened>`.
 *
 * *Note.* Often when talking about policies, people imply converting an event into a command, not
 * an event. This approach seems too complicated to us at this stage, as not many commands will do
 * anything but produce events with the same information, thus we directly convert between events.
 *
 * @param E the type of the event handled by this policy
 */
public abstract class Policy<E : EventMessage>(
    internal val external: Boolean = false
) : AbstractEventReactor(), Logging {

    private var context: BoundedContext? = null

    /**
     * Handles an event and produces some number of events in responce.
     */
    public abstract fun whenever(event: E): Iterable<EventMessage>

    final override fun registerWith(context: BoundedContext) {
        super.registerWith(context)
        this.context = context
    }

    /**
     * Creates a [QueryingClient] to find views of the given class.
     *
     * Users may create their own views and submit them via a [io.spine.protodata.plugin.Plugin].
     *
     * This method is targeted for Java API users. If you use Kotlin, see the no-param overload for
     * prettier code.
     */
    protected fun <P : EntityState> select(type: Class<P>): QueryingClient<P> {
        return QueryingClient(context!!, type, javaClass.name)
    }

    /**
     * Creates a [QueryingClient] to find views of the given type.
     *
     * Users may create their own views and submit them via a [io.spine.protodata.plugin.Plugin].
     */
    protected inline fun <reified P : EntityState> select(): QueryingClient<P> {
        val cls = P::class.java
        return select(cls)
    }

    final override fun messageClasses(): ImmutableSet<EventClass> {
        val classes = super.messageClasses()
        checkHandlers(classes)
        return classes
    }

    private fun checkHandlers(events: Iterable<EventClass>) {
        val classes = events.toList()
        if (classes.size > 1) {
            _error().log("Policy `%s` handles too many events: [%s].",
                         lazy { javaClass.name },
                         lazy { classes.joinToString() })
        }
    }
}
