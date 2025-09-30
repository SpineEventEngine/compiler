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

package io.spine.tools.compiler.jvm.style

import com.palantir.javaformat.java.Formatter
import com.palantir.javaformat.java.JavaFormatterOptions
import com.palantir.javaformat.java.JavaFormatterOptions.Style
import io.spine.tools.code.Java
import io.spine.tools.compiler.jvm.render.JavaRenderer
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.compiler.render.SourceFileSet
import io.spine.tools.compiler.render.forEachOfLanguage

/**
 * Java style formatter based on Palantir Java Format library.
 */
internal class PalantirJavaFormatter : JavaRenderer() {

    private val formatter by lazy {
        Formatter.createFormatter(
            JavaFormatterOptions.builder().style(Style.PALANTIR).build()
        )
    }

    override fun render(sources: SourceFileSet) {
        sources.forEachOfLanguage<Java> {
            reformat(it)
        }
    }

    private fun reformat(file: SourceFile<Java>) {
        val code = file.code()
        val formatted = formatter.formatSource(code)
        file.overwrite(formatted)
    }
}
