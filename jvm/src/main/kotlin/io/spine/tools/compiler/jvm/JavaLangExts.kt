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

@file:JvmName("JavaLangTypes")

package io.spine.tools.compiler.jvm

import io.spine.tools.java.isJavaLang
import io.spine.tools.java.reference

/**
 * Tells if this class belongs to the "java.lang" package.
 */
@Deprecated(
    message = "Please use `io.spine.tools.java.isJavaLang` instead.",
    replaceWith = ReplaceWith("isJavaLang",
        imports = ["io.spine.tools.java.isJavaLang"]
    )
)
public val Class<*>.isJavaLang: Boolean
    get() = name.contains("java.lang")

/**
 * Tells if this annotation type is repeatable.
 *
 * Since the receiver is the Java class, we check the presence of
 * [java.lang.annotation.Repeatable] annotation, not [kotlin.annotation.Repeatable].
 */
@Deprecated(
    message = "Please use `io.spine.tools.java.isRepeatable` instead.",
    replaceWith = ReplaceWith("isRepeatable",
        imports = ["io.spine.tools.java.isRepeatable"]
    )
)
public val <T: Annotation> Class<T>.isRepeatable: Boolean
    get() = isAnnotationPresent(java.lang.annotation.Repeatable::class.java)

/**
 * Obtains the code which is used for referencing this annotation class in Java code.
 *
 * @return a simple class name for the class belonging to `java.lang` package.
 *          Otherwise, a canonical name is returned.
 */
@Deprecated(message = "Please use `reference` instead.", replaceWith = ReplaceWith("reference"))
public val <T: Annotation> Class<T>.codeReference: String
    get() = reference

/**
 * Obtains the code which is used for referencing this class in Java code.
 *
 * @return a simple class name for the class belonging to `java.lang` package.
 *         Otherwise, a canonical name is returned.
 */
@Deprecated(
    message = "Please use `io.spine.tools.java.reference` instead.",
    replaceWith = ReplaceWith("reference",
        imports = ["io.spine.tools.java.reference"]
    )
)
public val <T: Any> Class<T>.reference: String
    get() = if (isJavaLang) simpleName else canonicalName
