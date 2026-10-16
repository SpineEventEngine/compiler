/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.tools.compiler.type

import com.google.protobuf.ByteString
import com.google.protobuf.ByteString.copyFromUtf8
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.base.fieldPath
import io.spine.tools.code.AnyLanguage
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.Type
import io.spine.tools.compiler.ast.fieldType
import io.spine.tools.compiler.ast.type
import io.spine.tools.compiler.test.TypesTestEnv.enumTypeName
import io.spine.tools.compiler.test.TypesTestEnv.messageTypeName
import io.spine.tools.compiler.value.EnumValue
import io.spine.tools.compiler.value.ListValue
import io.spine.tools.compiler.value.MapValue
import io.spine.tools.compiler.value.MapValueKt.entry
import io.spine.tools.compiler.value.MessageValue
import io.spine.tools.compiler.value.NullValue.NULL_VALUE
import io.spine.tools.compiler.value.Reference
import io.spine.tools.compiler.value.Value
import io.spine.tools.compiler.value.enumValue
import io.spine.tools.compiler.value.listValue
import io.spine.tools.compiler.value.mapValue
import io.spine.tools.compiler.value.messageValue
import io.spine.tools.compiler.value.reference
import io.spine.tools.compiler.value.value
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ValueConverter` should dispatch")
internal class ValueConverterSpec {

    private val converter = StubValueConverter()

    @Test
    fun nulls() {
        val typedNull = value {
            nullValue = NULL_VALUE
            type = type { primitive = TYPE_STRING }
        }
        converter.dispatchOf(typedNull) shouldBe "null:TYPE_STRING"
    }

    @Test
    fun bool() {
        converter.dispatchOf(value { boolValue = true }) shouldBe "bool:true"
    }

    @Test
    fun doubles() {
        converter.dispatchOf(value { doubleValue = .5 }) shouldBe "double:0.5"
    }

    @Test
    fun ints() {
        converter.dispatchOf(value { intValue = 42 }) shouldBe "int:42"
    }

    @Test
    fun strings() {
        converter.dispatchOf(value { stringValue = "hello" }) shouldBe "string:hello"
    }

    @Test
    fun bytes() {
        val bytes = value { bytesValue = copyFromUtf8("hi") }
        converter.dispatchOf(bytes) shouldBe "bytes:hi"
    }

    @Test
    fun messages() {
        val message = value {
            messageValue = messageValue { type = messageTypeName }
        }
        converter.dispatchOf(message) shouldBe "message:${messageTypeName.simpleName}"
    }

    @Test
    fun `enum constants`() {
        val constant = value {
            enumValue = enumValue {
                type = enumTypeName
                constNumber = 1
            }
        }
        converter.dispatchOf(constant) shouldBe "enum:${enumTypeName.simpleName}:1"
    }

    @Test
    fun lists() {
        val list = value {
            listValue = listValue {
                values += value { stringValue = "a" }
                values += value { stringValue = "b" }
            }
        }
        converter.dispatchOf(list) shouldBe "list:2"
    }

    @Test
    fun maps() {
        val map = value {
            mapValue = mapValue {
                value += entry {
                    key = value { stringValue = "k" }
                    this.value = value { stringValue = "v" }
                }
            }
        }
        converter.dispatchOf(map) shouldBe "map:1"
    }

    @Test
    fun references() {
        val ref = value {
            reference = reference {
                type = fieldType { primitive = TYPE_STRING }
                target = fieldPath { fieldName.add("bar") }
            }
        }
        converter.dispatchOf(ref) shouldBe "reference:bar"
    }

    /**
     * The `kind` oneof of `Value` is `(choice).required`, so a value with no kind
     * cannot be obtained from the DSL — building one fails validation before
     * [ValueConverter.valueToCode] is ever called. The default instance is
     * pre-built and skips validation, so it is the only way to reach
     * the rejecting branch.
     */
    @Test
    @DisplayName("and fail on a value with no kind set")
    fun failOnUnsetKind() {
        val error = shouldThrow<IllegalStateException> {
            converter.dispatchOf(Value.getDefaultInstance())
        }
        error.message shouldContain Value.KindCase.KIND_NOT_SET.name
    }
}

/**
 * Tags each conversion with the name of the abstract member to which
 * [ValueConverter] routed it, so that a spec can assert the dispatch rather
 * than the formatting of a particular target language.
 */
private class StubValueConverter : ValueConverter<AnyLanguage, StubCode>() {

    fun dispatchOf(value: Value): String = valueToCode(value).toCode()

    override fun nullToCode(type: Type): StubCode = StubCode("null:${type.primitive.name}")

    override fun toCode(value: Boolean): StubCode = StubCode("bool:$value")

    override fun toCode(value: Double): StubCode = StubCode("double:$value")

    override fun toCode(value: Long): StubCode = StubCode("int:$value")

    override fun toCode(value: String): StubCode = StubCode("string:$value")

    override fun toCode(value: ByteString): StubCode = StubCode("bytes:${value.toStringUtf8()}")

    override fun toCode(value: MessageValue): StubCode =
        StubCode("message:${value.type.simpleName}")

    override fun toCode(value: EnumValue): StubCode =
        StubCode("enum:${value.type.simpleName}:${value.constNumber}")

    override fun toList(value: ListValue): StubCode = StubCode("list:${value.valuesCount}")

    override fun toCode(value: MapValue): StubCode = StubCode("map:${value.valueCount}")

    override fun toCode(reference: Reference): StubCode =
        StubCode("reference:${reference.target.getFieldName(0)}")
}

private class StubCode(private val text: String) : CodeElement<AnyLanguage> {

    override fun toCode(): String = text
}
