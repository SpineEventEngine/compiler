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

package io.spine.protodata.renderer

import com.google.common.annotations.VisibleForTesting
import io.spine.string.Indent
import io.spine.string.Indent.Companion.DEFAULT_JAVA_INDENT_SIZE
import io.spine.string.Separator
import io.spine.string.atLevel
import io.spine.text.TextFactory.lineSplitter

/**
 * A fluent builder for inserting code into pre-prepared insertion points.
 *
 * @see SourceFile.at
 */
public class SourceAtLine
internal constructor(
    private val file: SourceFile,
    private val point: InsertionPoint,
    private val indent: Indent = Indent(DEFAULT_JAVA_INDENT_SIZE)
) {

    private var indentLevel: Int = 0

    /**
     * Specifies extra indentation to be added to inserted code lines
     *
     * Each unit adds the number of spaces specified by the [indent] property.
     */
    public fun withExtraIndentation(level: Int): SourceAtLine {
        require(level >= 0) { "Indentation level cannot be negative." }
        indentLevel = level
        return this
    }

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *         code lines.
     */
    public fun add(vararg lines: String): Unit =
        add(lines.toList())

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *         code lines.
     */
    public fun add(lines: Iterable<String>) {
        val sourceLines = file.lines()
        val updatedLines = ArrayList(sourceLines)
        val pointMarker = point.codeLine
        val newCode = lines.linesToCode(indent, indentLevel)
        val newLines = lineSplitter().splitToList(newCode)
        var alreadyInsertedCount = 0
        sourceLines.forEachIndexed { index, line ->
            if (line.contains(pointMarker)) {
                //   1. Take the index of the insertion point before any new code is added.
                //   2. Add the number of lines taken up by the code inserted above this line.
                //   3. Insert code at the next line, after the insertion point.
                val trueLineNumber = index + (alreadyInsertedCount * newLines.size) + 1
                updatedLines.addAll(trueLineNumber, newLines)
                alreadyInsertedCount++
            }
        }
        file.updateLines(updatedLines)
    }
}

/**
 * Joins these lines of code into a code block, accounting for extra indent.
 *
 * @param indent
 *         the number of spaces for each indentation.
 * @param indentLevel
 *         the number of levels of indentation to add.
 */
@VisibleForTesting
internal fun Iterable<String>.linesToCode(indent: Indent, indentLevel: Int): String =
    joinToString(Separator.nl()) {
        indent.atLevel(indentLevel) + it
    }
