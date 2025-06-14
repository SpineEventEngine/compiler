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

package io.spine.tools.compiler.jvm.render

import com.google.protobuf.Message
import io.spine.base.EntityState
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.compiler.render.TypeListActions
import io.spine.tools.code.Java

/**
 * An abstract base for Java renders handling message types.
 *
 * This class applies multiple render actions to multiple types.
 * For applying rendering actions to one type, please see [TypeRenderer].
 *
 * @param V The type of the view state which gathers messages types served by this renderer.
 *  The type is an [EntityState] that has [File] as its identifier and
 *  implements the [TypeListActions] interface.
 * @param S The type of the settings used by the renderer.
 *
 * @see TypeRenderer
 */
public abstract class TypeListRenderer<V, S : Message> : AbstractRenderer<V, S>()
        where V : EntityState<File>, V : TypeListActions {

    /**
     * Implement this method to render the code for the given entity state [type]
     * the source code of which present in the given [file].
     */
    protected abstract fun doRender(type: MessageType, file: SourceFile<Java>)

    final override fun doRender(view: V) {
        val types = view.getTypeList()
        types.forEach {
            val sourceFile = sources.javaFileOf(it)
            doRender(it, sourceFile)
        }
    }
}
