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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.spine.testing.UtilityClassTest
import io.spine.tools.compiler.render.TextFactory.createText
import io.spine.tools.compiler.render.TextFactory.text
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`TextFactory` should")
class TextFactoryTest: UtilityClassTest<TextFactory>(
    TextFactory::class.java) {

    private val nl = System.lineSeparator()

    @Test
    fun `find substring`() {
        val text = createText("abra", "ka", "dabra")

        assertThrows<IllegalArgumentException> { text.contains("abra${nl}ka") }

        text.contains("abra") shouldBe true
        text.contains("kada") shouldBe false
    }

    @Test
    fun `must not accept lines with separators`() {
        assertThrows<IllegalArgumentException> {  createText("un", "${nl}o") }
        assertThrows<IllegalArgumentException> {  text(listOf("dos", "tres${nl}")) }
    }

    @Test
    fun `always return the same value`() {
        val text = createText("donna", "be", "la", "mare")

        text.value shouldBeSameInstanceAs text.value
    }

    @Test
    fun `provide 'newLine()' shortcut method`() {
        TextFactory.newLine() shouldBeSameInstanceAs nl
    }
}
