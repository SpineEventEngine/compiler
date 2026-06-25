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

package io.spine.testing.compiler

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.spine.tools.compiler.Compilation
import java.io.File
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`assertCompilationError()` should")
internal class AssertionsSpec {

    private val file = File("Order.proto")
    private val line = 100
    private val column = 5
    private val message = "The ID field has an unsupported type."

    @Test
    fun `return the thrown compilation error`() {
        val (error, _) = assertCompilationError {
            Compilation.error(file, line, column, message)
        }
        error.message shouldContain message
    }

    @Test
    fun `capture the console output produced during compilation`() {
        val (_, output) = assertCompilationError {
            Compilation.error(file, line, column, message)
        }
        output shouldContain message
        output shouldContain "$line:$column"
    }

    @Test
    fun `fail when the action does not raise a compilation error`() {
        shouldThrow<AssertionError> {
            assertCompilationError {
                // Does nothing, so no `Compilation.Error` is raised.
            }
        }
    }
}
