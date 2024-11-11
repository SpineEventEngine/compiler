/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.java

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiStatement
import io.spine.tools.psi.java.Environment

/**
 * An executable Java instruction.
 *
 * An example of creating an arbitrary Java statement:
 *
 * ```
 * val printOne = Statement("System.out.println(1.0);")
 * ```
 */
public interface Statement : JavaElement

/**
 * Creates a new instance of [Statement] with the given [code].
 *
 * This function returns the [default][ArbitraryStatement] implementation
 * of [Statement].
 *
 * @param code The Java code denoting a statement.
 */
public fun Statement(code: String): Statement = ArbitraryStatement(code)

/**
 * An arbitrary Java statement.
 *
 * This is the basic and default implementation of [Statement].
 *
 * @param code The Java code denoting a statement.
 */
public open class ArbitraryStatement(code: String) : ArbitraryElement(code), Statement

/**
 * Creates a new [PsiStatement] from this Java [Statement].
 */
public fun Statement.toPsi(context: PsiElement? = null): PsiStatement =
    Environment.elementFactory.createStatementFromText(toCode(), context)
