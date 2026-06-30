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

package io.spine.tools.compiler.render

import io.spine.tools.compiler.context.Member
import io.spine.tools.code.Language

/**
 * A `Renderer` takes an existing [SourceFileSet] and modifies it,
 * changing the contents of existing source files, creating new ones, or
 * deleting unwanted files.
 *
 * Instances of `Renderer`s are usually created by
 * the [Plugin.renderers][io.spine.tools.compiler.plugin.Plugin.renderers] method.
 *
 * @see RenderAction
 */
public abstract class Renderer<L : Language>
protected constructor(language: L) : Member<L>(language) {

    /**
     * Performs required changes to the given source set.
     *
     * The renderer processes only the files written in its [language].
     * If [sources] contains files but none of them are in this language,
     * the set is assumed to belong to another language and is left untouched.
     *
     * An empty source set is still passed to [render] because a renderer may
     * create new files from scratch rather than modify the existing ones.
     */
    public fun renderSources(sources: SourceFileSet) {
        val relevantFiles = sources.subsetWhere { language.matches(it.relativePath) }

        // A non-empty source set with no files in this renderer's `language`
        // belongs to another language. Skip it so that, for example, a Java
        // renderer does not attempt to create or locate `.java` files under
        // a directory that holds generated Kotlin code.
        val belongsToAnotherLanguage = relevantFiles.isEmpty && !sources.isEmpty
        if (belongsToAnotherLanguage) {
            return
        }

        relevantFiles.prepareForQueries(this)
        render(relevantFiles)
        sources.mergeBack(relevantFiles)
    }

    /**
     * Makes changes to the given source set.
     *
     * The source set is guaranteed to consist only of the files, containing the code in
     * the supported programming [language].
     *
     * This method may be called several times if the Compiler is called with multiple
     * source and target directories.
     */
    protected abstract fun render(sources: SourceFileSet)
}
