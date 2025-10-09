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

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import io.spine.string.Separator
import io.spine.string.containsLineSeparators
import io.spine.string.escapeLineSeparators
import io.spine.tools.compiler.render.TextFactory.newLine
import java.util.regex.Pattern

/**
 * Static factories and precondition checks for creating instances of [Text].
 *
 * A recommended way for using this class is using its methods statically
 * imported, so that the creation of [Text] objects looks compact:
 *
 * ```kotlin
 * import io.spine.tools.compiler.render.TextFactory.text
 * ...
 * var twoLines = text("one", "two");
 * ```
 */
public object TextFactory {

    private val newLinePattern by lazy {
        Pattern.compile("\n|(\r\n)|\r")
    }

    internal val SPLITTER by lazy {
        Splitter.on(newLinePattern)
    }

    private val NL: String = Separator.nl()

    private val JOINER = Joiner.on(NL)

    /**
     * Creates a new instance with the given value.
     */
    @JvmStatic
    public fun text(value: String): Text = text {
        this.value = value
    }

    /**
     * Creates a new instance of text with lines separated by [line separator][newLine].
     *
     * @throws IllegalArgumentException
     * if one of the lines
     */
    @JvmStatic
    public fun text(lines: Iterable<String>): Text {
        checkNoSeparators(lines)
        val joined = JOINER.join(lines)
        return text(joined)
    }

    /**
     * Creates a new multi-line text with the given lines.
     *
     * @throws IllegalArgumentException if any of the lines contains a
     *  [line separator][containsLineSeparators]
     */
    @JvmStatic
    @VisibleForTesting
    public fun createText(vararg lines: String): Text =
        text(lines.asList())

    /**
     * Ensures that lines do not contain
     * [line separators][containsLineSeparators].
     *
     * @throws IllegalArgumentException if at least one line contains
     *   a [line separator][containsLineSeparators].
     */
    public fun checkNoSeparators(lines: Iterable<String>) {
        lines.forEach(::checkNoSeparator)
    }

    /**
     * Ensures that the charter sequence does not contain a
     * [line separator][containsLineSeparators].
     *
     * @throws IllegalArgumentException if the sequence contains a
     *   [line separator][containsLineSeparators]
     */
    @JvmStatic
    public fun checkNoSeparator(line: CharSequence) {
        require(!line.containsLineSeparators()) {
            "Unexpected line separators found in the string: `${line.escapeLineSeparators()}`."
        }
    }

    /**
     * Obtains line separator used in the operating system.
     *
     * Use this method for brevity of code related to working with lines.
     */
    @JvmStatic
    public fun newLine(): String = NL
}
