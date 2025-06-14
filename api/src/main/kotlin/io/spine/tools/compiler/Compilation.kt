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

import com.google.common.annotations.VisibleForTesting
import io.spine.base.Mistake
import io.spine.environment.Tests
import io.spine.tools.compiler.Compilation.ERROR_EXIT_CODE
import io.spine.tools.compiler.Compilation.error
import io.spine.tools.compiler.ast.Span
import io.spine.tools.compiler.ast.toJava
import io.spine.string.Indent
import io.spine.string.Separator
import io.spine.string.indent
import java.io.File
import java.io.Serial
import kotlin.system.exitProcess
import io.spine.tools.compiler.ast.File as PFile

/**
 * Provides functions to report compilation errors and warnings.
 *
 * The object has two modes of handling the compilation [error].
 * In the production mode, the [error] method prints an error message to [System.err] and
 * exits the process with [ERROR_EXIT_CODE].
 *
 * In the testing mode, the [error] method throws [Compilation.Error] exception with
 * the same error message as printed to the console in the production mode.
 *
 * The execution mode is [detected][Tests.enabled] via the [Tests] environment type.
 *
 * @see Tests
 */
public object Compilation {

    /**
     * Tells if the code generation is performed under tests.
     */
    private val underTests: Boolean
        get() = Tests.type().enabled()

    /**
     * The exit code for a compilation error.
     */
    public const val ERROR_EXIT_CODE: Int = -1

    /**
     * The prefix used for error messages.
     */
    @VisibleForTesting
    internal const val ERROR_PREFIX = "e:"

    /**
     * The prefix used for warning messages.
     */
    @VisibleForTesting
    internal const val WARNING_PREFIX = "w:"

    /**
     * Prints the error diagnostics to [System.err] and terminates the compilation.
     *
     * The error message includes the path to the [file], line and column numbers
     * followed by the [message].
     *
     * The termination of the compilation in the production mode is done by
     * exiting the process with [ERROR_EXIT_CODE].
     *
     * If the code is run [under tests][Tests] the method throws [Compilation.Error].
     *
     * @param file The file in which the error occurred.
     * @param line The one-based number of the line with the error.
     * @param column The one-based number of the column with the error.
     * @param message The description of what went wrong.
     * @throws Compilation.Error exception when called under tests.
     */
    @Suppress("TooGenericExceptionThrown") // False positive from detekt.
    public fun error(file: File, line: Int, column: Int, message: String): Nothing {
        val output = errorMessage(file, line, column, message)
        System.err.println(output)
        if (underTests) {
            throw Error(output)
        } else {
            exitProcess(ERROR_EXIT_CODE)
        }
    }

    /**
     * Prints the error diagnostics to [System.err] and terminates the compilation.
     *
     * The termination of the compilation in the production mode is done by
     * exiting the process with [ERROR_EXIT_CODE].
     *
     * If the code is run [under tests][Tests] the method throws [Compilation.Error].
     *
     * @param file The file in which the error occurred.
     * @param line The one-based number of the line with the error.
     * @param column The one-based number of the column with the error.
     * @param message The description of what went wrong.
     * @throws Compilation.Error exception when called under tests.
     */
    public fun error(file: PFile, line: Int, column: Int, message: String): Nothing =
        error(file.toJava(), line, column, message)

    /**
     * Prints the error diagnostics to [System.err] and terminates the compilation.
     *
     * The termination of the compilation in the production mode is done by
     * exiting the process with [ERROR_EXIT_CODE].
     *
     * If the code is run [under tests][Tests] the method throws [Compilation.Error].
     *
     * @param file The file in which the error occurred.
     * @param span The span of the Protobuf declaration which caused the error.
     * @param message The function calculating the description of what went wrong.
     * @throws Compilation.Error exception when called under tests.
     */
    public fun error(file: PFile, span: Span, message: () -> String): Nothing =
        error(file, span.startLine, span.startColumn, message())

    @VisibleForTesting
    internal fun errorMessage(file: File, line: Int, column: Int, message: String) =
        indentedMessage(ERROR_PREFIX, file, line, column, message)

    /**
     * Prints the warning diagnostics to [System.out].
     *
     * The method returns the string printed to the console so that it could be also
     * put into logging output by the calling code.
     *
     * @param file The file which causes the warning.
     * @param line The one-based number of the line with the questionable code.
     * @param column The one-based number of the column with the questionable code.
     * @param message The description of the warning.
     * @return the string printed to the console.
     */
    public fun warning(file: File, line: Int, column: Int, message: String): String {
        val output = warningMessage(file, line, column, message)
        println(output)
        return output
    }

    /**
     * Prints the warning diagnostics to [System.out]
     *
     * The method returns the string printed to the console so that it could be also
     * put into logging output by the calling code.
     *
     * @param file The file which causes the warning.
     * @param span The span of the questionable Protobuf declaration.
     * @param message The function calculating the description of the warning.
     * @return the string printed to the console.
     */
    public fun warning(file: PFile, span: Span, message: () -> String): String =
        warning(file, span.startLine, span.startColumn, message())

    /**
     * Prints the warning diagnostics to [System.out]
     *
     * The method returns the string printed to the console so that it could be also
     * put into logging output by the calling code.
     *
     * @param file The file which causes the warning.
     * @param line The one-based number of the line with the questionable code.
     * @param column The one-based number of the column with the questionable code.
     * @param message The description of the warning.
     * @return the string printed to the console.
     */
    public fun warning(file: PFile, line: Int, column: Int, message: String): String =
        warning(file.toJava(), line, column, message)

    @VisibleForTesting
    internal fun warningMessage(file: File, line: Int, column: Int, message: String) =
        indentedMessage(WARNING_PREFIX, file, line, column, message)

    /**
     * Constructs a formatted compilation message with indentation
     * for multi-line user messages.
     *
     * The returned string consists of:
     *
     * 1. The specified [prefix] followed by the file URI or path, line, and column.
     * 2. The first line of the [message] appended directly to the header.
     * 3. Any subsequent lines of the [message], each indented under the header
     *    by the length of the [prefix] plus one space.
     *
     * @throws IllegalArgumentException if [prefix] is blank.
     */
    @VisibleForTesting
    internal fun indentedMessage(
        prefix: String,
        file: File,
        line: Int,
        column: Int,
        message: String
    ): String {
        require(prefix.isNotBlank()) {
            "The compilation message must have a non-empty prefix specified."
        }

        val messageLines = message.lines()
        val messageHeader = messageLines.first()

        // "+1" counts whitespace between a prefix and the file path.
        val indent = Indent(prefix.length + 1)

        // The first line is appended to the location, each consequent line is
        // prepended with indentation.
        val indentedBody = messageLines.drop(1)
            .indent(indent, level = 1)

        // The method uses OS-specific line breaks.
        return buildString {
            append("$prefix ${file.maybeUri()}:$line:$column: $messageHeader")
            if (indentedBody.isNotEmpty()) {
                append(Separator.nl())
                append(indentedBody)
            }
        }
    }

    /**
     * The exception thrown by [Compilation.error] when the testing mode is on.
     */
    public class Error(message: String) : Mistake(message) {
        public companion object {
            @Serial
            private const val serialVersionUID: Long = 0L
        }
    }
}

/**
 * Invokes [Compilation.error] if the [condition] is `false`, does nothing otherwise.
 *
 * @param condition If `false`, the [Compilation.error] will be called.
 * @param file The file causing the error.
 * @param span The span of the declaration which cases the error.
 * @param message The lazily evaluated part of the error message.
 *
 * @see Compilation.error
 */
public fun Compilation.check(condition: Boolean, file: PFile, span: Span, message: () -> String) {
    if (!condition) {
        error(file, span, message)
    }
}

/**
 * Converts the path of this file into a URI if the path is absolute.
 *
 * The purpose of this function is to make it easier to locate a file with
 * an error or a warning. When a file URI is used for the console output,
 * it could be opened in an IDE or a browser.
 *
 * If the path is relative, it is simply returned as the result of this function.
 * Even though file URI could be
 * [relative](https://stackoverflow.com/questions/7857416/file-uri-scheme-and-relative-files)
 * we do not want to use this because its full path would be resolved relatively
 * to a user home directory or the current directory.
 * None of these cases represent a directory with proto files.
 * Therefore, we just print a relative file name to avoid the confusion.
 */
private fun File.maybeUri(): String = if (isAbsolute) {
    toURI().toString().replace(NO_HOSTNAME_PREFIX, EMPTY_HOSTNAME_PREFIX)
} else {
    path
}

/**
 * The prefix used in a file URI if
 * [host name is not used](https://en.wikipedia.org/wiki/File_URI_scheme).
 */
@VisibleForTesting
internal const val NO_HOSTNAME_PREFIX = "file:/"

/**
 * The prefix used in a file URI for an
 * [empty host name](https://en.wikipedia.org/wiki/File_URI_scheme).
 *
 * We use this prefix because it is recognized by the IntelliJ IDEA
 * console as a clickable URI.
 * Kotlin compiler also uses this prefix for reporting compilation errors.
 */
@VisibleForTesting
internal const val EMPTY_HOSTNAME_PREFIX = "file:///"
