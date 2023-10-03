/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.protodata.renderer

import io.spine.base.EntityState
import io.spine.protodata.config.ConfiguredQuerying
import io.spine.protodata.type.TypeSystem
import io.spine.server.BoundedContext
import io.spine.server.ContextAware
import io.spine.server.query.QueryingClient
import io.spine.tools.code.Language

/**
 * A `Renderer` takes an existing [SourceFileSet] and modifies it, changing the contents of existing
 * source files, creating new ones, or deleting unwanted files.
 *
 * Instances of `Renderer`s are usually created by
 * the [Plugin.renderers()][io.spine.protodata.plugin.Plugin.renderers] method.
 */
public abstract class Renderer<L : Language>
protected constructor(
    private val language: L
) : ConfiguredQuerying, ContextAware {

    private lateinit var codegenContext: BoundedContext
    private lateinit var typeSystem: TypeSystem

    /**
     * Performs required changes to the given source set.
     */
    public fun renderSources(sources: SourceFileSet) {
        val relevantFiles = sources.subsetWhere { language.matches(it.relativePath) }
        relevantFiles.prepareForQueries(this)
        render(relevantFiles)
        sources.mergeBack(relevantFiles)
    }

    /**
     * Makes changes to the given source set.
     *
     * The source set is guaranteed to consist only of the files, containing the code in
     * the [language].
     *
     * This method may be called several times, if ProtoData is called with multiple source and
     * target directories.
     */
    protected abstract fun render(sources: SourceFileSet)

    public final override fun <P : EntityState<*>> select(type: Class<P>): QueryingClient<P> {
        return QueryingClient(codegenContext, type, javaClass.name)
    }

    final override fun <T> configAs(cls: Class<T>): T = super.configAs(cls)

    final override fun configIsPresent(): Boolean = super.configIsPresent()

    /**
     * Injects the context of the ProtoData application.
     *
     * This method is `public` but is essentially `internal` to ProtoData SDK.
     *
     * @see 
     */
    public override fun registerWith(codegenContext: BoundedContext) {
        if (isRegistered) {
            check(this.codegenContext == codegenContext) {
                "Unable to register the renderer `$this` with" +
                        " `${codegenContext.name().value}`." +
                        " The renderer is already registered with" +
                        " `${this.codegenContext.name().value}`."
            }
            return
        }
        this.codegenContext = codegenContext
    }

    override fun isRegistered(): Boolean {
        return this::codegenContext.isInitialized
    }

    internal fun withTypeSystem(typeSystem: TypeSystem) {
        this.typeSystem = typeSystem
    }
}
