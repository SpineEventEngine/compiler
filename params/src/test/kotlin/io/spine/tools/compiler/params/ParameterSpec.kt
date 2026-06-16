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

package io.spine.tools.compiler.params

import com.google.common.testing.EqualsTester
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`Parameter` should")
internal class ParameterSpec {

    @Test
    fun `expose the long and short names of the parameters`() {
        ParametersFileParam.name shouldBe "--params"
        ParametersFileParam.shortName shouldBe "-P"

        InfoLoggingParam.name shouldBe "--info"
        InfoLoggingParam.shortName shouldBe "-I"

        DebugLoggingParam.name shouldBe "--debug"
        DebugLoggingParam.shortName shouldBe "-D"
    }

    @Test
    fun `trim the indentation of the help text`() {
        // The `help` property must not retain the indentation of the source code.
        ParametersFileParam.help shouldStartWith "The path to the file"
        ParametersFileParam.help shouldContain "pb.json"
        InfoLoggingParam.help shouldStartWith "Set log level to `INFO`."
        DebugLoggingParam.help shouldStartWith "Set log level to `DEBUG`."
    }

    @Test
    fun `provide 'toString' returning the long name`() {
        ParametersFileParam.toString() shouldBe "--params"
        InfoLoggingParam.toString() shouldBe "--info"
        DebugLoggingParam.toString() shouldBe "--debug"
    }

    @Test
    fun `compare parameters by instance identity`() {
        EqualsTester()
            .addEqualityGroup(ParametersFileParam)
            .addEqualityGroup(InfoLoggingParam)
            .addEqualityGroup(DebugLoggingParam)
            .testEquals()
    }
}
