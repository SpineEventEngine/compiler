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

package io.spine.protodata.codegen.java

import assertCode
import com.google.protobuf.Empty
import io.spine.protodata.FieldKt.ofMap
import io.spine.protodata.FieldName
import io.spine.protodata.PrimitiveType
import io.spine.protodata.Types
import io.spine.protodata.field
import io.spine.protodata.fieldName
import io.spine.protodata.typeName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`MessageReference` should")
internal class MessageReferenceSpec {

    @Test
    fun `print name`() {
        val referenceName = "value"
        val messageReference = MessageReference(referenceName)
        assertCode(messageReference, referenceName)
    }

    @Test
    fun `access a singular field`() {
        val messageReference = MessageReference(LABEL)
        val field = field {
            name = fieldName
            type = Types.string
            declaringType = typeName
            single = Empty.getDefaultInstance()
        }
        val fieldAccess = messageReference.field(field)
        assertCode(fieldAccess.getter, "$LABEL.getBaz()")
    }

    @Test
    fun `access a list field`() {
        val messageReference = MessageReference(LABEL)
        val field = field {
            name = fieldName
            type = Types.string
            declaringType = typeName
            list = Empty.getDefaultInstance()
        }
        val fieldAccess = messageReference.field(field)
        assertCode(fieldAccess.getter, "$LABEL.getBazList()")
    }

    @Test
    fun `access a map field`() {
        val messageReference = MessageReference(LABEL)
        val field = field {
            name = fieldName
            type = Types.string
            declaringType = typeName
            map = ofMap {
                keyType = PrimitiveType.TYPE_STRING
            }
        }
        val fieldAccess = messageReference.field(field)
        assertCode(fieldAccess.getter, "$LABEL.getBazMap()")
    }
}

const val LABEL = "msg"

private val fieldName: FieldName = fieldName {
    value = "baz"
}

private val typeName: io.spine.protodata.TypeName = typeName {
    simpleName = "StubType"
    packageName = "given.message"
}
