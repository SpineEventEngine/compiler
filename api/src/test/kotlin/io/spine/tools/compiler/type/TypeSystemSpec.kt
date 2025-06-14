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

package io.spine.tools.compiler.type

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.spine.tools.compiler.ast.EnumType
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.Service
import io.spine.tools.compiler.ast.serviceName
import io.spine.tools.compiler.ast.typeName
import io.spine.tools.compiler.test.TypesTestEnv.enumTypeName
import io.spine.tools.compiler.test.TypesTestEnv.messageTypeName
import io.spine.tools.compiler.test.TypesTestEnv.multipleFilesHeader
import io.spine.tools.compiler.test.TypesTestEnv.serviceNameMultiple
import io.spine.tools.compiler.test.TypesTestEnv.typeSystem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`TypeSystem` should")
class TypeSystemSpec {

    @Nested inner class
    find {
        @Test
        fun `message by name`() {
            val (messageType, file) = typeSystem.findMessage(messageTypeName)!!
            messageType shouldNotBe null
            file shouldNotBe null

            messageType.name shouldBe messageTypeName
            file shouldBe multipleFilesHeader
        }

        @Test
        fun `enum by name`() {
            val (enumType, file) = typeSystem.findEnum(enumTypeName)!!
            enumType shouldNotBe null
            file shouldNotBe null

            enumType.name shouldBe enumTypeName
            file shouldBe multipleFilesHeader
        }

        @Test
        fun `enum or message type by name`() {
            val (enumType, _) = typeSystem.findMessageOrEnum(enumTypeName)!!
            enumType.shouldBeInstanceOf<EnumType>()

            val (messageType, _) = typeSystem.findMessageOrEnum(messageTypeName)!!
            messageType.shouldBeInstanceOf<MessageType>()
        }

        @Test
        fun `service by name`() {
            val (service, _) = typeSystem.findService(serviceNameMultiple)!!
            service.shouldBeInstanceOf<Service>()
        }
    }

    @Nested inner class
    `not find` {

        @Test
        fun `message type by enum name`() {
            val declaration = typeSystem.findMessage(enumTypeName)
            declaration shouldBe null
        }

        @Test
        fun `enum type by message name`() {
            val declaration = typeSystem.findEnum(messageTypeName)
            declaration shouldBe null
        }

        @Test
        fun `unknown type`() {
            val declaration = typeSystem.findMessageOrEnum(typeName {
                simpleName = "ThisTypeIsUnknown"
                packageName = "com.acme"
            })
            declaration shouldBe null
        }

        @Test
        fun `service by unknown name`() {
            val service = typeSystem.findService(serviceName {
                simpleName = "ThisServiceIsUnknown"
                packageName = "com.acme"
            })
            service shouldBe null
        }
    }
}
