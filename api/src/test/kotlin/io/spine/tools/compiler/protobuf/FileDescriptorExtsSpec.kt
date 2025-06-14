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

package io.spine.tools.compiler.protobuf

import com.google.protobuf.AnyProto
import com.google.protobuf.TimestampProto
import io.kotest.matchers.shouldBe
import io.spine.tools.compiler.test.ImportsTestProto
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`FileDescriptor` extensions should")
internal class FileDescriptorExtsSpec {

    /** The descriptor of `google/protobuf/any.proto`. */
    private val anyProto = AnyProto.getDescriptor()

    /** The descriptor of `imports_test.proto`. */
    private val importsTestProto = ImportsTestProto.getDescriptor()

    /** The descriptor of `google/protobuf/timestamp.proto`. */
    private val timestampProto = TimestampProto.getDescriptor()

    @Test
    fun `obtain a total number of imports of the file`() {
        anyProto.importCount shouldBe 0
        importsTestProto.importCount shouldBe 3
    }

    @Test
    fun `tell if a file is imported by another`() {
        anyProto.isImportedBy(importsTestProto) shouldBe true
        timestampProto.isImportedBy(importsTestProto) shouldBe true

        importsTestProto.run {
            isImportedBy(anyProto) shouldBe false
            isImportedBy(timestampProto) shouldBe false
        }
    }
}
