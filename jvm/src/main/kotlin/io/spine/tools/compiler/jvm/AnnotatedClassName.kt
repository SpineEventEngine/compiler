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
 * An annotated [ClassName].
 *
 * For simple names, this class places the annotation before the simple name:
 *
 * ```
 * val string = ClassName(packageName = "", "String")
 * val nullable = ClassName(Nullable::class)
 * val annotatedString = AnnotatedClassName(string, nullable)
 * println(annotatedString) // @org.checkerframework.checker.nullness.qual.Nullable String
 * ```
 *
 * For fully qualified class names and classes with multiple simple names, the annotation
 * is placed before the last simple name:
 *
 * ```
 * val entry = ClassName(Map.Entry::class)
 * val nullable = ClassName(Nullable::class)
 * val annotatedEntry = AnnotatedClassName(entry, nullable)
 * println(annotatedString) // java.util.Map.@org.checkerframework.checker.nullness.qual.Nullable Entry
 * ```
 *
 * @param [className] The class name to annotate.
 * @param [annotation] The class name of the annotation to apply.
 */
public class AnnotatedClassName(
    className: ClassName,
    annotation: ClassName
) : JavaTypeName() {

    override val canonical: String = className.canonical.replaceAfterLast(
        delimiter = ".",
        replacement = "@${annotation.canonical} ${className.simpleName}",
        missingDelimiterValue = "@${annotation.canonical} ${className.canonical}"
    )
}
