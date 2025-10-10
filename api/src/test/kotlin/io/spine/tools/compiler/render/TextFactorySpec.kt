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

package io.spine.tools.compiler.render

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.string.Separator
import io.spine.testing.UtilityClassTest
import io.spine.tools.compiler.render.TextFactory.createText
import io.spine.tools.compiler.render.TextFactory.newLine
import io.spine.tools.compiler.render.TextFactory.text
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`TextFactory` should")
internal class TextFactorySpec : UtilityClassTest<TextFactory>(TextFactory::class.java) {

    private val nl = Separator.nl()

    companion object {
        val linesWithSeparators = listOf(
            " Fiz ${Separator.LF.value} buz? ",
            " Foo ${Separator.CR.value} bar.",
            "Ka ${Separator.CRLF.value} boom!"
        )
    }

    @Nested
    @DisplayName("prohibit line separators when building from many lines")
    inner class Prohibition {

        @Test
        fun `with an array`() = assertThrowsOn {
            text(linesWithSeparators)
        }

        @Test
        fun `with an 'Iterable'`() = assertThrowsOn {
            text(linesWithSeparators.toList())
        }

        @Test
        fun `with vararg`() = assertThrowsOn {
            createText(linesWithSeparators[0], linesWithSeparators[1])
        }

        private fun assertThrowsOn(fn: () -> Unit) {
            assertThrows<IllegalArgumentException> {
                fn.invoke()
            }
        }
    }

    @Test
    fun `allow creating text with raw string value`() {
        val rawText = linesWithSeparators.joinToString(separator = nl)
        assertDoesNotThrow {
            text(rawText)
        }
    }

    @Test
    fun `split text into lines`() {
        val str = "uno${nl}dos${nl}tres"
        val text = text(str)

        text.lines() shouldContainInOrder listOf("uno", "dos", "tres")
    }

    @Test
    fun `split literals into lines`() {
        val str = """
           foo
            bar
            baxxx
        """.trimIndent()
        val text = text(str)

        text.lines() shouldContainInOrder listOf("foo", " bar", " baxxx")
    }

    @Test
    fun `split text with mixed separators into lines`() {
        val str = "1\r2\r\n3\r4"
        val text = text(str)

        text.lines() shouldContainInOrder listOf("1", "2", "3", "4")
    }

    @Test
    fun `join 'Iterable'`() {
        val iterable = listOf("bir", "iki", "üç")
        val text = text(iterable)

        text.value shouldBe "bir${nl}iki${nl}üç"
    }

    @Test
    fun `provide shortcut method for obtaining system line separator`() {
        newLine() shouldBe System.lineSeparator()
    }
}
