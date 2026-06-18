/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.testing.compiler

import io.spine.logging.testing.tapConsole
import io.spine.tools.compiler.Compilation
import org.junit.jupiter.api.assertThrows

/**
 * Asserts that the given [action] fails compilation, returning the thrown error
 * together with the console output it produced.
 *
 * The console output is captured via [tapConsole] — so the deliberately provoked
 * compilation error does not pollute the build log — and returned as the second
 * component of the pair, letting callers inspect the diagnostics if they need to.
 *
 * @param action The code expected to fail with a [Compilation.Error].
 * @return A pair of the thrown [Compilation.Error] and the captured console output.
 * @see tapConsole
 */
public fun assertCompilationError(action: () -> Unit): Pair<Compilation.Error, String> {
    lateinit var error: Compilation.Error
    val output = tapConsole {
        error = assertThrows<Compilation.Error> {
            action()
        }
    }
    return error to output
}
