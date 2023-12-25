/*
 * Copyright 2023, TeamDev. All rights reserved.
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

@file:Suppress("MatchingDeclarationName")
@file:JvmName("ProtocInsertionPoints")

package io.spine.protodata.codegen.java

import io.spine.protodata.TypeName
import io.spine.protodata.renderer.InsertionPoint
import io.spine.protodata.renderer.ProtocInsertionPoint

/**
 * Standard [InsertionPoint]s generated for a message, or an enum type by the Protobuf compiler.
 */
public enum class TypedInsertionPoint {

    /**
     * The place where class-level definitions go.
     *
     * Only available for message types.
     */
    CLASS_SCOPE,

    /**
     * The place where class-level definitions go in a builder class.
     *
     * Only available for message types.
     */
    BUILDER_SCOPE,

    /**
     * The place where class-level definition goes in an enum class.
     *
     * Only available for enum types.
     */
    ENUM_SCOPE,

    /**
     * The place where implemented interfaces go.
     *
     * Only available for message types.
     *
     * Remember to add a comma after the interface name when using this insertion point.
     * For example:
     * ```
     *   corp.acme.BuilderMixin,
     * ```
     */
    MESSAGE_IMPLEMENTS,

    /**
     * The place where implemented interfaces go in a builder class.
     *
     * Only available for message types.
     *
     * Remember to add a comma after the interface name when using this insertion point.
     * For example:
     * ```
     *   corp.acme.BuilderMixin,
     * ```
     */
    BUILDER_IMPLEMENTS;

    /**
     * Obtains the [InsertionPoint] of the associated scope for the given type.
     */
    public fun forType(typeName: TypeName): InsertionPoint {
        val scopeName = name.lowercase()
        return ProtocInsertionPoint(scopeName, typeName)
    }
}

/**
 * The standard [InsertionPoint] generated by the Protobuf compiler in the Java outer class for
 * every Protobuf file.
 *
 * Note that the insertion point is identical for each outer class.
 */
public val OUTER_CLASS_SCOPE: InsertionPoint = ProtocInsertionPoint("outer_class_scope")
