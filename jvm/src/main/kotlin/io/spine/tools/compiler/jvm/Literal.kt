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

package io.spine.tools.compiler.jvm

/**
 * An arbitrary literal.
 *
 * Such an expression should denote a constant, compile-time known value.
 *
 * For example:
 *
 * ```
 * val yes = Literal<Boolean>("true")
 * val half = Literal<Double>("0.5")
 * val ten = Literal<Int>("10")
 * ```
 *
 * @param [value] A string representation of the literal. It will be used "as is"
 *  in the resulting Java code.
 *
 * @see StringLiteral
 */
public open class Literal<T>(value: T) : Expression<T>("$value")

/**
 * A string literal.
 *
 * Represents the same value as the given string, wrapped in quotation marks.
 * No extra character escaping is performed.
 */
public class StringLiteral(value: String) : Literal<String>("\"$value\"")

/**
 * A `long` literal.
 *
 * Represents the same value as the given long, followed by `L` symbol.
 * For example, for "12" it will produce the following code: "12L".
 */
public class LongLiteral(value: Long) : Literal<String>("${value}L")
