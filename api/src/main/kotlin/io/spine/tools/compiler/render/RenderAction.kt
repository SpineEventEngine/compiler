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

package io.spine.tools.compiler.render

import com.google.protobuf.Message
import io.spine.tools.compiler.ast.EnumType
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.ProtoDeclaration
import io.spine.tools.compiler.ast.Service
import io.spine.tools.compiler.ast.TypeDeclaration
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.context.Member
import io.spine.tools.compiler.type.TypeSystem
import io.spine.tools.code.Language

/**
 * A base class for classes that modify the code of a single source file.
 *
 * Render actions participate in the source code rendering process and
 * are called by [Renderer]s either directly or indirectly.
 *
 * @param L The type of the programming language served by this action.
 * @param D The type of the Protobuf declaration, such as
 *   [MessageType][io.spine.tools.compiler.ast.MessageType],
 *   [EnumType][io.spine.tools.compiler.ast.EnumType] or
 *   [Service][io.spine.tools.compiler.ast.Service], for which this action generates the code.
 * @param P The type of the parameter passed to the action.
 *   If the action does not have a parameter, please use [com.google.protobuf.Empty].
 *
 * @param language The language served by this action.
 * @property subject The Protobuf declaration served by this action.
 * @property file The source code file to be modified by this action.
 * @property parameter The parameter passed to the action.
 * @property context The code generation context in which this action operates.
 *
 * @see Renderer
 */
public abstract class RenderAction<L : Language, D : ProtoDeclaration, P : Message>(
    language: L,
    protected val subject: D,
    protected val file: SourceFile<L>,
    protected val parameter: P,
    protected final override val context: CodegenContext
) : Member<L>(language) {

    init {
        registerWith(context)
    }

    /**
     * A type system with the Protobuf types defined in the current code generation pipeline.
     */
    protected final override val typeSystem: TypeSystem by lazy { context.typeSystem }

    /**
     * Renders the code in the given source file.
     */
    public abstract fun render()
}

/**
 * A render action performed for a Protobuf type,
 * such as [MessageType][io.spine.tools.compiler.ast.MessageType] or
 * [EnumType][io.spine.tools.compiler.ast.EnumType].
 *
 * @param L The type of the programming language served by this action.
 * @param T The type of the Protobuf type declaration, such as
 *   [MessageType][io.spine.tools.compiler.ast.MessageType] or
 *   [EnumType][io.spine.tools.compiler.ast.EnumType],
 *   for which this action generates the code.
 * @param P The type of the parameter passed to the action.
 *   If the action does not have a parameter, please use [com.google.protobuf.Empty].
 *
 * @param language The language served by this action.
 * @property type The message or enum type for which this action works.
 *  This property would have the same value as [subject], and is added for readability in
 *  templates for the generated code.
 * @param file The source code file to be modified by this action.
 * @param parameter The parameter passed to the action.
 * @param context The code generation context in which this action operates.
 *
 * @see MessageAction
 * @see EnumAction
 */
public abstract class TypeAction<L : Language, T : TypeDeclaration, P : Message>
protected constructor(
    language: L,
    protected val type: T,
    file: SourceFile<L>,
    parameter: P,
    context: CodegenContext
) : RenderAction<L, T, P>(language, type, file, parameter, context)

/**
 * A render action performed for a [MessageType][io.spine.tools.compiler.ast.MessageType].
 *
 * @param L The type of the programming language served by this action.
 * @param P The type of the parameter passed to the action.
 *   If the action does not have a parameter, please use [com.google.protobuf.Empty].
 *
 * @param language The language served by this action.
 * @param type The message type for which this action works.
 * @param file The source code file to be modified by this action.
 * @param parameter The parameter passed to the action.
 * @param context The code generation context in which this action operates.
 *
 * @see EnumAction
 * @see ServiceAction
 */
public abstract class MessageAction<L : Language, P : Message>(
    language: L,
    type: MessageType,
    file: SourceFile<L>,
    parameter: P,
    context: CodegenContext
) : TypeAction<L, MessageType, P>(language, type, file, parameter, context)

/**
 * A render action performed for an [EnumType][io.spine.tools.compiler.ast.EnumType].
 *
 * @param L the programming language supported by this action.
 * @param P the type of the parameter passed to the action.
 *   If the action does not have a parameter, please use [com.google.protobuf.Empty].
 *
 * @param language The programming language served by this action.
 * @param type The enum type served by this action.
 * @param file The source code file to be modified by this action.
 * @param parameter The parameter passed to the action.
 * @param context The code generation context in which this action runs.
 *
 * @see MessageAction
 * @see ServiceAction
 */
public abstract class EnumAction<L : Language, P : Message>(
    language: L,
    type: EnumType,
    file: SourceFile<L>,
    parameter: P,
    context: CodegenContext
) : TypeAction<L, EnumType, P>(language, type, file, parameter, context)

/**
 * A render action performed for a [Service][io.spine.tools.compiler.ast.Service].
 *
 * @param L the programming language supported by this action.
 *
 * @param language The programming language served by this action.
 * @property service The service declaration handled by this action.
 *   This property would have the same value as [subject], and is added for readability in
 *   templates for the generated code.
 * @param file The source code file to be modified by this action.
 * @param parameter The parameter passed to the action.
 * @param context The code generation context in which this action runs.
 *
 * @see MessageAction
 * @see EnumAction
 */
public abstract class ServiceAction<L : Language, P : Message>(
    language: L,
    /**
     * The same as `subject`, added for readability in generated code templates.
     */
    protected val service: Service,
    file: SourceFile<L>,
    parameter: P,
    context: CodegenContext
) : RenderAction<L, Service, P>(language, service, file, parameter, context)
