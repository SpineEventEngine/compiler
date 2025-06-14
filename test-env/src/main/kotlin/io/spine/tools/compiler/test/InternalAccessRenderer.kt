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

package io.spine.tools.compiler.test

import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.render.Renderer
import io.spine.tools.compiler.render.SourceFileSet
import io.spine.server.query.Querying
import io.spine.server.query.select
import io.spine.tools.code.Java
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Creates a new package-private class for each [InternalType].
 */
public class InternalAccessRenderer : Renderer<Java>(Java) {

    override fun render(sources: SourceFileSet) {
        val internalTypes = (this as Querying).select<InternalType>().all()
        internalTypes.forEach { internalType ->
            val path = internalType.toPath()
            sources.createFile(
                path,
                """
                class ${internalType.name.simpleName}Internal {
                    // Here goes case specific code.
                }
                """.trimIndent()
            )
        }
    }
}

private fun InternalType.toPath(): Path {
    val path = qualifiedName.replace('.', File.separatorChar)
    return Path("${path}Internal.java")
}

private val InternalType.qualifiedName: String
    get() = name.qualifiedName
