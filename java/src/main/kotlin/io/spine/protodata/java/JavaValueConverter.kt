/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.java

import com.google.protobuf.ByteString
import com.google.protobuf.Message
import io.spine.protodata.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.protodata.ast.Type
import io.spine.protodata.ast.Type.KindCase.ENUMERATION
import io.spine.protodata.ast.Type.KindCase.MESSAGE
import io.spine.protodata.ast.Type.KindCase.PRIMITIVE
import io.spine.protodata.ast.cardinality
import io.spine.protodata.java.FieldMethods.Companion.getterOf
import io.spine.protodata.type.ValueConverter
import io.spine.protodata.value.EnumValue
import io.spine.protodata.value.ListValue
import io.spine.protodata.value.MapValue
import io.spine.protodata.value.MessageValue
import io.spine.protodata.value.Reference
import io.spine.tools.code.Java

/**
 * A [ValueConverter] which converts values into Java expressions.
 */
@Suppress("TooManyFunctions")
public class JavaValueConverter(
    private val convention: MessageOrEnumConvention
) : ValueConverter<Java, Expression<*>>() {

    override fun nullToCode(type: Type): Null = Null

    override fun toCode(value: Boolean): Expression<Boolean> = Literal(value)

    override fun toCode(value: Double): Expression<Double> = Literal(value)

    override fun toCode(value: Long): Expression<Long> = Literal(value)

    override fun toCode(value: String): StringLiteral = StringLiteral(value)

    override fun toCode(value: ByteString): LiteralBytes = LiteralBytes(value)

    override fun toCode(value: MessageValue): MethodCall<Message> {
        val type = value.type
        val className = convention.declarationFor(type).name as ClassName
        return if (value.fieldsMap.isEmpty()) {
            className.getDefaultInstance()
        } else {
            var builder = className.newBuilder()
            value.fieldsMap.forEach { (k, v) ->
                builder = builder.chainSet(k, valueToCode(v))
            }
            builder.chainBuild()
        }
    }

    override fun toCode(value: EnumValue): MethodCall {
        val type = value.type
        val enumClassName = convention.declarationFor(type).name as EnumName
        return enumClassName.enumValue(value.constNumber)
    }

    override fun toList(value: ListValue): MethodCall {
        val expressions = value.valuesList.map(this::valueToCode)
        return listExpression(expressions)
    }

    override fun toCode(value: MapValue): MethodCall {
        val valueList = value.valueList
        val firstEntry = valueList.firstOrNull()
        val firstKey = firstEntry?.key
        val keyClass = firstKey?.type?.toClass() as ClassName?
        val firstValue = firstEntry?.value
        val valueClass = firstValue?.type?.toClass() as ClassName?
        val valuesMap = valueList.associate {
            valueToCode(it.key) to valueToCode(it.value)
        }
        return mapExpression(valuesMap, keyClass, valueClass)
    }

    override fun toCode(reference: Reference): MethodCall {
        val path = reference.target.fieldNameList.toMutableList()

        // If the field reference contains only one element, take the cardinality of the field type.
        // We will have only one getter call.
        val startCardinality = if (path.size == 1) {
            reference.type.cardinality
        } else {
            // Otherwise, only message types fields are expected in the path until the last entry.
            CARDINALITY_SINGLE
        }
        val start = path.removeFirst()

        // Assume we generate the call in the scope of a message method.
        var call = MethodCall(InstanceScope, getterOf(start, startCardinality))

        // The remaining path (if any) would be chained method calls.
        path.forEachIndexed() { index, field ->
            // For all fields but last we can have only message type fields.
            call = if (index == path.size - 1) {
                call.chain(getterOf(field, CARDINALITY_SINGLE))
            } else {
                // The last field in the path has the type (and
                // the cardinality) of the "source" one.
                call.chain(getterOf(field, reference.type.cardinality))
            }
        }
        return call
    }

    private fun Type.toClass(): ClassOrEnumName = when (kindCase) {
        MESSAGE -> convention.declarationFor(message).name
        ENUMERATION -> convention.declarationFor(enumeration).name
        PRIMITIVE -> primitive.toJavaClass()
        else -> error("Expected a valid type.")
    }
}
