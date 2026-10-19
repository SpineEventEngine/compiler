/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

@file:JvmName("FilePatterns")

package io.spine.tools.compiler.ast

import io.spine.tools.compiler.ast.FilePattern.KindCase.KIND_NOT_SET
import io.spine.tools.compiler.ast.FilePattern.KindCase.REGEX
import io.spine.tools.compiler.ast.FilePattern.KindCase.INFIX
import io.spine.tools.compiler.ast.FilePattern.KindCase.SUFFIX

/**
 * Tells if this pattern matches the given [file].
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
    }
}

/**
 * Tells if this file pattern matches the file in which the given [type] is declared.
 */
public fun FilePattern.matches(type: MessageType): Boolean =
    matches(type.file)
