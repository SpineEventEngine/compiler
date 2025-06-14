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

package io.spine.tools.compiler

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.spine.tools.compiler.Compilation.ERROR_PREFIX
import io.spine.tools.compiler.Compilation.WARNING_PREFIX
import io.spine.tools.compiler.ast.Span
import io.spine.tools.compiler.ast.toAbsoluteFile
import io.spine.logging.testing.tapConsole
import io.spine.string.ti
import io.spine.string.tm
import io.spine.testing.TestValues
import java.io.File
import java.nio.file.Paths
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`Compilation` object should")
internal class CompilationSpec {

    @Test
    fun `throw the 'Error' exception under tests`() {
        val file = File("some.proto").absoluteFile
        val lineNumber = 100
        val columnNumber = 500
        val errorMessage = "Some error."
        tapConsole {
            val exception = assertThrows<Compilation.Error> {
                Compilation.error(file, lineNumber, columnNumber, errorMessage)
            }
            exception.message.let {
                it shouldContain EMPTY_HOSTNAME_PREFIX
                it shouldContain file.uriRef()
                it shouldContain "$lineNumber:$columnNumber"
                it shouldContain errorMessage
            }
        }
    }

    @Test
    fun `print the error message to the system error stream`() {
        val file = Paths.get("nested/dir/file.proto").toFile().absoluteFile
        val lineNumber = 10
        val columnNumber = 5
        val errorMessage = "Testing console output."
        val consoleOutput = tapConsole {
            assertThrows<Compilation.Error> {
                Compilation.error(file, lineNumber, columnNumber, errorMessage)
            }
        }
        consoleOutput.let {
            it shouldContain EMPTY_HOSTNAME_PREFIX // because the file path is absolute.
            it shouldContain file.uriRef()
            it shouldContain "$lineNumber:$columnNumber"
            it shouldContain errorMessage
        }
    }

    @Test
    fun `print the warning message to the system out stream`() {
        val file = Paths.get("nested/dir/file.proto").toFile().absoluteFile
        val lineNumber = 10
        val columnNumber = 5
        val errorMessage = "Testing console output."
        val consoleOutput = tapConsole {
            assertThrows<Compilation.Error> {
                Compilation.error(file, lineNumber, columnNumber, errorMessage)
            }
        }
        consoleOutput.let {
            it shouldContain EMPTY_HOSTNAME_PREFIX // because the file path is absolute.
            it shouldContain file.uriRef()
            it shouldContain "$lineNumber:$columnNumber"
            it shouldContain errorMessage
        }
    }

    @Test
    fun `use file URI for absolute paths`() {
        val file = File("/some/dir/file.proto").absoluteFile
        Compilation.errorMessage(file, 1, 2, "").let {
            it shouldContain EMPTY_HOSTNAME_PREFIX
            it shouldContain file.uriRef()
        }
        Compilation.warningMessage(file, 3, 4, "").let {
            it shouldContain EMPTY_HOSTNAME_PREFIX
            it shouldContain file.uriRef()
        }
    }

    @Test
    fun `use relative file path if it is not absolute`() {
        val file = File("not/absolute/file.proto")
        Compilation.errorMessage(file, 1, 2, "").let {
            it shouldNotContain EMPTY_HOSTNAME_PREFIX
            it shouldNotContain NO_HOSTNAME_PREFIX
            it shouldContain file.path // system-dependent file name.
        }
        Compilation.warningMessage(file, 3, 4, "").let {
            it shouldNotContain EMPTY_HOSTNAME_PREFIX
            it shouldNotContain NO_HOSTNAME_PREFIX
            it shouldContain file.path // system-dependent file name.
        }
    }

    @Test
    fun `use the prefix for error messages`() {
        val file = File("with_error.proto")
        Compilation.errorMessage(file, 1, 2, "").let {
            it shouldStartWith ERROR_PREFIX
        }
    }

    @Test
    fun `use the prefix for warning messages`() {
        val file = File("with_warning.proto")
        Compilation.warningMessage(file, 3, 4, "").let {
            it shouldStartWith WARNING_PREFIX
        }
    }

    @Test
    fun `provide the 'check' utility function`() {
        val file = File("some/path/goes/here.proto").toAbsoluteFile()
        val span = Span.getDefaultInstance()
        val msg = TestValues.randomString()
        val error = assertThrows<Compilation.Error> {
            Compilation.check(condition = false, file, span) { msg }
        }
        
        error.message shouldContain msg

        assertDoesNotThrow {
            Compilation.check(condition = true, file, span) { msg }
        }
    }

    @Nested inner class
    `when indenting a user message` {

        private val file = File("with_error.proto")
        private val prefix = "m:"
        private val line = 1
        private val column = 1

        @Test
        fun `use the prefix length as a number of spaces`() {
            val message = """
                The file contains one or more mistakes.
                Mistake #1: illegal beginning.
                Mistake #2: illegal ending.
            """.ti()

            val result = Compilation.indentedMessage(prefix, file, line, column, message)
            val expected = """
                m: with_error.proto:1:1: The file contains one or more mistakes.
                   Mistake #1: illegal beginning.
                   Mistake #2: illegal ending.
            """.ti()

            result shouldBe expected
        }

        @Test
        fun `throw when given an empty prefix`() {
            assertThrows<IllegalArgumentException> {
                Compilation.indentedMessage(prefix = "", file, line, column, message = "")
            }
        }

        @Test
        fun `do not append trailing lines for one-line messages`() {
            val message = "The file contains one or more mistakes."
            val result = Compilation.indentedMessage(prefix, file, line, column, message)
            result shouldBe "m: with_error.proto:1:1: The file contains one or more mistakes."
        }

        @Test
        fun `do nothing for empty messages`() {
            val result = Compilation.indentedMessage(prefix, file, line, column, message = "")
            result shouldBe "m: with_error.proto:1:1: "
        }

        @Test
        fun `preserve original blank lines and whitespaces`() {
            val message = """
                First line.

                Third line after blank.
                  Fourth line with its own whitespaces.
            """.ti()

            val result = Compilation.indentedMessage(prefix, file, line, column, message)
            val expected = """
                |m: with_error.proto:1:1: First line.
                |   
                |   Third line after blank.
                |     Fourth line with its own whitespaces.
            """.tm()

            result shouldBe expected
        }
    }
}

private fun File.uriRef(): String {
    val uriSeparator = "/"
    val separator = File.separator
    return if (separator == uriSeparator) {
        path
    } else {
        path.replace(separator, uriSeparator)
    }
}
