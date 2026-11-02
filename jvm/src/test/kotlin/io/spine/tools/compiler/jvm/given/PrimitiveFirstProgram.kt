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

package io.spine.tools.compiler.jvm.given

import io.spine.tools.compiler.jvm.JavaTypeName
import io.spine.tools.compiler.jvm.JavaTypeName.BOOLEAN
import io.spine.tools.compiler.jvm.JavaTypeName.BYTE
import io.spine.tools.compiler.jvm.JavaTypeName.CHAR
import io.spine.tools.compiler.jvm.JavaTypeName.DOUBLE
import io.spine.tools.compiler.jvm.JavaTypeName.FLOAT
import io.spine.tools.compiler.jvm.JavaTypeName.INT
import io.spine.tools.compiler.jvm.JavaTypeName.LONG
import io.spine.tools.compiler.jvm.JavaTypeName.SHORT
import io.spine.tools.compiler.jvm.JavaTypeName.VOID
import kotlin.system.exitProcess

/**
 * A program which initializes [JavaTypeName.INT] before the [JavaTypeName] class.
 *
 * The program exits with a non-zero code if [JavaTypeName.KnownPrimitives]
 * differs from the list of all Java primitive types.
 *
 * Run it via [runInFreshJvm] so that no Java code model class is initialized beforehand.
 */
internal object PrimitiveFirstProgram {

    @JvmStatic
    fun main(@Suppress("unused") args: Array<String>) {
        // This is the first access to the `JavaTypeName` hierarchy in the JVM.
        val first: JavaTypeName = INT
        val primitives = JavaTypeName.KnownPrimitives
        val expected = listOf(VOID, BOOLEAN, BYTE, SHORT, first, LONG, CHAR, FLOAT, DOUBLE)
        if (primitives != expected) {
            System.err.println("`KnownPrimitives` is `$primitives` instead of `$expected`.")
            exitProcess(1)
        }
    }
}
