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

package io.spine.protodata.codegen.java.file

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import io.spine.protodata.codegen.java.ClassOrEnumName
import io.spine.string.Separator
import io.spine.text.Text
import io.spine.tools.psi.convertLineSeparators
import io.spine.tools.psi.java.Environment
import io.spine.tools.psi.java.Parser
import io.spine.tools.psi.java.locate

/**
 * Prints the lines of the text into a single string using the system line separator.
 */
internal fun Text.printLines(): String =
        lines().joinToString(separator = Separator.system.value)

/**
 * Obtains the instance of [PsiFile] for this text.
 */
public fun Text.psiFile(): PsiJavaFile =
    TextToPsiParser.get(this)

/**
 * Locates a class or an enum with the given [name] in the [Text].
 *
 * @return the instance of [PsiClass] if found, `null` otherwise.
 */
public fun Text.locate(name: ClassOrEnumName): PsiClass? {
    val psiFile = psiFile()
    val psiClass = psiFile.locate(name.simpleNames)
    return psiClass
}

/**
 * The cache which allows avoiding repeated parsing of a [Text] instance for
 * obtaining corresponding [PsiJavaFile].
 */
private object TextToPsiParser: CacheLoader<Text, PsiJavaFile>() {

    private const val LIMIT = 300L

    private val parser by lazy {
        Environment.setUp()
        Parser(Environment.project)
    }

    private val cache: LoadingCache<Text, PsiJavaFile> =
        CacheBuilder.newBuilder()
            .maximumSize(LIMIT)
            .build(TextToPsiParser)

    fun get(text: Text): PsiJavaFile = cache.get(text)

    override fun load(key: Text): PsiJavaFile =
        parser.parse(key.value.convertLineSeparators())
}
