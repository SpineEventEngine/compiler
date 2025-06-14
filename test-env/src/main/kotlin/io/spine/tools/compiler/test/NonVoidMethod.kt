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

package io.spine.tools.compiler.test

import io.spine.tools.compiler.render.InsertionPoint
import io.spine.tools.compiler.render.InsertionPointPrinter
import io.spine.text.TextCoordinates
import io.spine.tools.code.Java
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * An insertion point marking a public non-void instance method.
 */
public class NonVoidMethod : InsertionPoint {

    private companion object {
        @Suppress("MaxLineLength")
        private val publicInstanceMethodPattern = Regex(
            "\\s*public\\s+((final)|(abstract)\\s+)?(synchronized)?(?!(class)|(@?interface)|(enum)|(void)).+\\s*",
            DOT_MATCHES_ALL
        )
    }

    override val label: String
        get() = "NonVoidMethod"

    override fun locate(text: String): Set<TextCoordinates> =
        text.lines()
            .asSequence()
            .mapIndexed { index, line -> index to line }
            .filter { publicInstanceMethodPattern.matches(it.second) }
            .map { atLine(it.first - 1) }
            .toSet()
}

/**
 * A printer for [NonVoidMethod].
 */
public class NonVoidMethodPrinter : InsertionPointPrinter<Java>(Java, setOf(NonVoidMethod()))
