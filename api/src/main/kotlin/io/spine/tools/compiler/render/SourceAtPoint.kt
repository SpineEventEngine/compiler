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

import io.spine.collect.interlaced
import io.spine.text.TextFactory

/**
 * A fluent builder for inserting code into pre-prepared inline insertion points.
 *
 * @see SourceFile.atInline
 */
public sealed interface SourceAtPoint {
    /**
     * Adds the specified code fragment into the insertion point.
     *
     * When called multiple times for the same insertion point, the code that is added last
     * will appear first in the file, since new code fragments are always added right after
     * the insertion point regardless of if it's been used before.
     */
    public fun add(codeFragment: String)
}

/**
 * A [SourceAtPoint] at a point that could not be located.
 *
 * No code is generated by this implementation.
 */
internal object NoOp : SourceAtPoint {
    override fun add(codeFragment: String) = Unit
}

/**
 * A [SourceAtPoint] at a point at a specific point or points in the file.
 *
 * The points are defined by the insertion point specified via the [point].
 */
internal class SpecificPoint(
    private val file: SourceFile<*>,
    private val point: InsertedPoint
) : SourceAtPoint {

    override fun add(codeFragment: String) {
        TextFactory.checkNoSeparator(codeFragment)
        val sourceLines = file.lines()
        val updatedLines = ArrayList(sourceLines)
        sourceLines.asSequence()
            .mapIndexed { index, line -> CodeLine(index, line) }
            .map { line -> line.insertInline(point, codeFragment) }
            .forEach { (index, line) -> updatedLines[index] = line }
        file.updateLines(updatedLines)
    }
}

/**
 * A numbered line of code in a file.
 */
private data class CodeLine(val lineIndex: Int, val content: String) {

    /**
     * Inserts the `newCode` at the given `insertionPoint` into this line and obtains the resulting
     * line of code.
     *
     * The index of the new line is always the same as the index of the old line.
     */
    fun insertInline(point: InsertedPoint, newCode: String): CodeLine {
        val indexes = content.findInsertionIndexes(point).toList()
        if (indexes.isEmpty()) {
            return this
        }
        val parts = content.splitByIndexes(indexes)
        val newLine = parts.interlaced(newCode).joinToString(separator = "")
        return CodeLine(lineIndex, newLine)
    }
}

private fun String.splitByIndexes(indexes: List<Int>): List<String> = buildList {
    val idxs = buildList(indexes.size + 2) {
        add(0)
        addAll(indexes)
        add(length)
    }
    idxs.forEachIndexed { listIndex, stringIndex ->
        if (listIndex < idxs.size - 1) {
            val nextIndex = idxs[listIndex + 1]
            add(substring(stringIndex, nextIndex))
        }
    }
}

private fun String.findInsertionIndexes(point: InsertedPoint): List<Int> {
    val comment = point.representationInCode
    var index = 0
    return buildList {
        while (true) {
            val rawIndex = indexOf(comment, startIndex = index)
            if (rawIndex >= 0) {
                index = rawIndex + comment.length
                add(index)
            } else {
                break
            }
        }
    }
}
