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

@file:JvmName("FilePatterns")

package io.spine.tools.compiler.ast

import io.spine.tools.compiler.ast.FilePattern.KindCase.KIND_NOT_SET
import io.spine.tools.compiler.ast.FilePattern.KindCase.REGEX
import io.spine.tools.compiler.ast.FilePattern.KindCase.INFIX
import io.spine.tools.compiler.ast.FilePattern.KindCase.SUFFIX

/**
 * Tells if this patterns matches the given [file].
 */
public fun FilePattern.matches(file: File): Boolean {
    val path = file.path
    @Suppress("DEPRECATION") // Support the `PREFIX` kind for backward compatibility.
    return when (kindCase) {
        FilePattern.KindCase.PREFIX -> path.startsWith(prefix)
        INFIX -> path.contains(infix)
        SUFFIX -> path.endsWith(suffix)
        REGEX -> path.matches(Regex(regex))
        KIND_NOT_SET -> false
        else -> return false
    }
}

/**
 * Tells if this file pattern matches the file in which the given [type] is declared.
 */
public fun FilePattern.matches(type: MessageType): Boolean =
    matches(type.file)
