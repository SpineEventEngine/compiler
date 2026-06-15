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

package io.spine.tools.compiler.params

import java.io.File.pathSeparator

/**
 * A parameter passed to the Compiler command-line application.
 *
 * ## Equality
 *
 * This class intentionally does not override `equals()` or `hashCode()`,
 * relying on the identity-based semantics inherited from [Any].
 *
 * Every subtype is a Kotlin `object` — a singleton — so each parameter has
 * exactly one instance (see [ParametersFileParam], [InfoLoggingParam], and
 * [DebugLoggingParam]). Two distinct instances of the same parameter cannot
 * exist; identity equality is therefore both correct and the only achievable
 * behavior. A value-based `equals()` would add no distinguishing power, so it
 * is deliberately omitted rather than maintained as effectively dead code.
 */
public sealed class Parameter(

    /**
     * A long name of the parameter, which usually comes with the `--` prefix.
     */
    public val name: String,

    /**
     * A short name of the parameter, which conventionally comes with the `-` prefix
     * if the short name is one letter, and with `--` prefix for two or more letters.
     */
    public val shortName: String,

    /**
     * Description of the parameter with the usage instructions which
     * could be passed as a raw string.
     */
    help: String
) {

    /**
     * Description of the parameter with the usage instructions.
     */
    public val help: String = help.trimIndent()

    final override fun toString(): String = name

    internal companion object {

        /**
         * The abbreviation for [pathSeparator] for using inside help texts.
         */
        val ps: String = pathSeparator
    }
}
