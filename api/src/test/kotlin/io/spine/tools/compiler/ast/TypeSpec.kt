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

package io.spine.tools.compiler.ast

import com.google.protobuf.Empty
import com.google.protobuf.Value
import io.kotest.matchers.shouldBe
import io.spine.base.ListOfAnys
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BOOL
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_SFIXED64
import io.spine.tools.compiler.protobuf.field
import io.spine.tools.compiler.protobuf.toType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`Type` should")
internal class TypeSpec {

    private val messageType: Type
    private val enumType: Type
    private val primitiveType: Type
    private val mapValueType: Type
    private val listItemType: Type

    init {
        val msg = io.spine.base.Error.getDescriptor()
        messageType = msg.field("details").toType()
        enumType = Value.getDescriptor().field("null_value").toType()
        primitiveType = msg.field("code").toType()
        mapValueType = msg.field("attributes").type.map.valueType
        listItemType = ListOfAnys.getDescriptor().field("value").type.list
    }

    @Nested inner class
    `obtain simple type name` {

        @Test
        fun `of 'Message' type`() {
            messageType.simpleName shouldBe "Any"
        }

        @Test
        fun `of enum type`() {
            enumType.simpleName shouldBe "NullValue"
        }

        @Test
        fun `rejecting for primitive types`() {
            assertThrows<IllegalStateException> {
                primitiveType.simpleName
            }
        }
    }

    @Nested inner class
    `obtain a type name of` {

        @Test
        fun message() {
            val msg = Empty.getDescriptor()
            msg.toType().name shouldBe msg.fullName
        }

        @Test
        fun enum() {
            val enum = PrimitiveType.getDescriptor()
            enum.toType().name shouldBe enum.fullName
        }

        @Test
        fun primitive() {
            TYPE_BOOL.toType().name shouldBe "bool"
            TYPE_DOUBLE.toType().name shouldBe "double"
            TYPE_SFIXED64.toType().name shouldBe "sfixed64"
        }
    }

    /**
     * This test checks the returned value supporting the reference in
     * the documentation of [TypeBase.primitive].
     */
    @Test
    fun `return 'PT_UNKNOWN' when is not primitive`() {
        val msg = com.google.protobuf.Any.getDescriptor()
        msg.toType().primitive shouldBe PrimitiveType.PT_UNKNOWN
    }
}
