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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.given.Driver
import io.spine.tools.compiler.ast.given.Tractor
import io.spine.tools.compiler.given.value.Student
import io.spine.tools.compiler.protobuf.toField
import io.spine.tools.compiler.protobuf.toMessageType
import io.spine.option.OptionsProto
import io.spine.protobuf.field
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
@DisplayName("`Field` extensions should")
internal class FieldExtsSpec {

    @Nested inner class
    `check if a field is` {

        @Test
        fun list() {
            val field = Field.newBuilder()
                .setType(fieldType {
                    list = TYPE_STRING.toType()
                })
                .buildPartial()

            field.isList shouldBe true
        }

        @Test
        fun map() {
            val field = Field.newBuilder()
                .setType(fieldType {
                    map = mapEntryType {
                        keyType = TYPE_STRING
                        valueType = TYPE_STRING.toType()
                    }
                })
                .buildPartial()

            field.isMap shouldBe true
        }

        @Suppress("DEPRECATION") // testing deprecated API.
        @Test
        fun `not repeated`() {
            val field = Field.newBuilder()
                .setType(fieldType { primitive = TYPE_STRING })
                .buildPartial()

            field.isRepeated shouldBe false
        }
    }

    @Test
    fun `add field documentation`() {
        val field = Tractor.getDescriptor().toMessageType().field("driver")
        val doc = field.doc
        doc.run {
            leadingComment shouldContain "This is the leading comment"
            trailingComment shouldContain "This is the trailing comment"
            detachedCommentList.find { it.contains("detached comment") } shouldNotBe null
        }
    }

    @Test
    fun `add field coordinates`() {
        val field = Driver.getDescriptor().toMessageType().field("license_number")
        val span = field.span
        span.run {
            startLine shouldBe 69
            startColumn shouldBe 5
            endLine shouldBe 69
            endColumn shouldBe 51
        }
    }

    @Test
    fun `find the option by its generated extension type`() {
        val field = Student.getDescriptor()
            .field(Student.ID_FIELD_NUMBER)!!
            .toField()
        field.findOption(OptionsProto.setOnce) shouldNotBe null
    }
}
