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

package io.spine.tools.compiler.jvm

import com.google.protobuf.Message
import io.spine.tools.compiler.ast.Cardinality
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.tools.compiler.ast.FieldName
import io.spine.tools.compiler.ast.fieldName

/**
 * A selector to access methods for a Protobuf message field.
 *
 * Depending on the field type, it may allow generating getters and setters for the field.
 */
public class FieldAccess
internal constructor(
    public val message: Expression<Message>,
    name: FieldName,
    kind: Cardinality,
) : FieldConventions(name, kind) {

    /**
     * Constructs field access for the given [message] and [name].
     */
    internal constructor(
        message: Expression<Message>,
        name: String,
        kind: Cardinality = CARDINALITY_SINGLE
    ) : this(message, fieldName { value = name }, kind)

    /**
     * A getter expression for the associated field.
     */
    public fun <T> getter(): MethodCall<T> = MethodCall(message, getterName)

    /**
     * Constructs a setter expression for the associated field.
     */
    public fun setter(value: Expression<*>): MethodCall<Message.Builder> =
        MethodCall(message, setterName, value)

    /**
     * Constructs an `addField(..)` expression for the associated field.
     */
    public fun add(value: Expression<*>): MethodCall<Message.Builder> =
        MethodCall(message, addName, value)

    /**
     * Constructs an `addAllField(..)` expression for the associated field.
     */
    public fun addAll(value: Expression<*>): MethodCall<Message.Builder> =
        MethodCall(message, addAllName, value)

    /**
     * Constructs an `putField(..)` expression for the associated field.
     */
    public fun put(key: Expression<*>, value: Expression<*>): MethodCall<Message.Builder> =
        MethodCall(message, putName, listOf(key, value))

    /**
     * Constructs an `putAllField(..)` expression for the associated field.
     */
    public fun putAll(value: Expression<*>): MethodCall<Message.Builder> =
        MethodCall(message, putAllName, value)

    override fun toString(): String {
        return "FieldAccess[$message#${name.value}]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldAccess) return false
        if (!super.equals(other)) return false

        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }
}
