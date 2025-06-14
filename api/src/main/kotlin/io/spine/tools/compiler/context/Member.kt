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

package io.spine.tools.compiler.context

import io.spine.annotation.Internal
import io.spine.base.EntityState
import io.spine.tools.compiler.ast.EnumInFile
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.MessageInFile
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.ProtobufSourceFile
import io.spine.tools.compiler.ast.ServiceInFile
import io.spine.tools.compiler.ast.enums
import io.spine.tools.compiler.ast.messages
import io.spine.tools.compiler.ast.services
import io.spine.tools.compiler.settings.LoadsSettings
import io.spine.tools.compiler.type.TypeSystem
import io.spine.server.query.Querying
import io.spine.server.query.QueryingClient
import io.spine.server.query.select
import io.spine.tools.code.Language

/**
 * A part of [CodegenContext] which participates in the code generation process and
 * may have settings it can load.
 *
 * @param L The type of the programming language served by this member.
 */
public abstract class Member<L : Language>
protected constructor(
    /**
     * The programming language served by this member.
     *
     * As most implementations of [Language] are Kotlin `object`s,
     * like [io.spine.tools.code.Java] or [io.spine.tools.code.Kotlin], it is likely that
     * the value passed to this parameter would repeat the argument specified
     * for the generic parameter [L].
     */
    public val language: L
) : LoadsSettings, ContextAware {

    /**
     * The backing field for the [context] property.
     */
    private lateinit var _context: CodegenContext

    /**
     * A code generation context associated with this instance.
     *
     * @throws IllegalStateException if accessed before `Code Generation` context
     *  is [injected][registerWith].
     */
    protected open val context: CodegenContext by lazy {
        checkContext(this::context.name)
        _context
    }

    /**
     * A type system with the Protobuf types defined in the current code generation pipeline.
     *
     * @throws IllegalStateException if accessed before `Code Generation` context
     *  is [injected][registerWith].
     */
    protected open val typeSystem: TypeSystem by lazy {
        checkContext(this::typeSystem.name)
        context.typeSystem
    }

    /**
     * Creates a [QueryingClient] for obtaining entity states of the given type.
     *
     * @param S the type of the entity state.
     */
    @Deprecated(
        "Please use `Querying.select()` extension instead.",
        ReplaceWith("select<S>()", imports = ["io.spine.server.query.select"])
    )
    public inline fun <reified S : EntityState<*>> select(): QueryingClient<S> =
        select(S::class.java)

    /**
     * Creates a [QueryingClient] for obtaining entity states of the given type.
     *
     * @param S The type of the entity state.
     * @param type The class of the entity state.
     */
    public final override fun <S : EntityState<*>> select(type: Class<S>): QueryingClient<S> =
        _context.select(type)

    final override fun <T : Any> loadSettings(cls: Class<T>): T = super.loadSettings(cls)

    final override fun settingsAvailable(): Boolean = super.settingsAvailable()

    /**
     * Injects the `Code Generation` context into this instance.
     *
     * The reference to the context is necessary to query the state of entities.
     *
     * This method is `public` because it is inherited from the [ContextAware] interface.
     * But it is essentially `internal` to the Compiler SDK, and is not supposed to be called
     * by authors of plugins directly.
     *
     * @see [select]
     * @see [io.spine.tools.compiler.backend.Pipeline]
     *
     * @suppress This function is not supposed to be used by plugin authors code.
     */
    @Internal
    public final override fun registerWith(context: CodegenContext) {
        if (isRegistered()) {
            check(_context == context) {
                "Unable to register `$this` with `${context}` because" +
                        " it is already registered with `${this._context}`."
            }
            return
        }
        _context = context
    }

    /**
     * Checks if this member is registered with the context.
     *
     * @suppress Similarly to [registerWith], this function is not supposed to be called by
     *  plugin authors users.
     */
    @Internal
    override fun isRegistered(): Boolean {
        return this::_context.isInitialized
    }

    private fun checkContext(property: String) = check(this::_context.isInitialized) {
        "Access to `${this::class.simpleName}.$property` property is not allowed until " +
                "the `Code Generation` context has been injected. Please invoke " +
                "`registerWith(CodegenContext)` method first."
    }
}

/**
 * Obtains the proto source file with the given [path].
 *
 * @param path The path to the source file which could be relative or absolute.
 * @return the found source file message, or `null` if the file was not found.
 */
public fun Member<*>.findSource(path: File): ProtobufSourceFile? {
    return (this as Querying).select<ProtobufSourceFile>().findById(path)
        ?: findAllFiles().firstOrNull {
            it.file.path.endsWith(path.path)
        }
}

/**
 * Obtains the header of the proto file with the given [path].
 *
 * @param path The path to the source file which could be relative or absolute.
 * @return the found header, or `null` if the file was not found.
 */
public fun Member<*>.findHeader(path: File): ProtoFileHeader? = findSource(path)?.header

/**
 * Obtains all Protobuf source code files passed to the current compilation process.
 */
public fun Member<*>.findAllFiles(): Collection<ProtobufSourceFile> =
    (this as Querying).select<ProtobufSourceFile>().all()

/**
 * Obtains all the message types that are parsed by the current compilation process
 * along with the corresponding file headers.
 *
 * Message types that are dependencies of the compilation process are not included.
 *
 * @see ProtobufSourceFile
 * @see io.spine.tools.compiler.ast.ProtobufDependency
 */
public fun Member<*>.findMessageTypes(): Set<MessageInFile> =
    findAllFiles()
        .flatMap { it.messages() }
        .toSet()

/**
 * Obtains all the enum types that are parsed by the current compilation process
 * along with the corresponding file headers.
 *
 * Enum types that are dependencies of the compilation process are not included.
 *
 * @see ProtobufSourceFile
 * @see io.spine.tools.compiler.ast.ProtobufDependency
 */
public fun Member<*>.findEnumTypes(): Set<EnumInFile> =
    findAllFiles()
        .flatMap { it.enums() }
        .toSet()

/**
 * Obtains all service declarations that are parsed by the current compilation process
 * along with the corresponding file headers.
 *
 * Services that are dependencies of the compilation process are not included.
 *
 * @see ProtobufSourceFile
 * @see io.spine.tools.compiler.ast.ProtobufDependency
 */
public fun Member<*>.findServices(): Set<ServiceInFile> =
    findAllFiles()
        .flatMap { it.services() }
        .toSet()
