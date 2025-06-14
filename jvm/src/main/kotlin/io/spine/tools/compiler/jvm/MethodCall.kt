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

package io.spine.tools.compiler.jvm

import com.google.protobuf.Message

/**
 * An expression of a Java method call.
 *
 * Can be a static or an instance method. In the case of the former, the scope is a class name.
 * In the case of the latter — an object reference. If the scope is empty, the method call is
 * considered as the one using an implicit receiver.
 *
 * @param T The method returned type.
 *
 * @param scope The receiver of the method call. Can be a class name or an object reference.
 *   It can be empty. In this case, the method call is done with an implicit receiver.
 * @param name The name of the method.
 * @param arguments The list of the arguments passed to the method.
 * @param generics The list of the type arguments passed to the method.
 */
public open class MethodCall<T> @JvmOverloads constructor(
    private val scope: JavaElement,
    name: String,
    arguments: List<Expression<*>> = listOf(),
    generics: List<JavaTypeName> = listOf()
) : Expression<T>(
    buildString {
        if (scope.toCode().isNotBlank()) {
            append("${scope.toCode()}.")
        }
        append(generics.genericTypes())
        append(name)
        append("(${arguments.formatParams()})")
    }
) {

    /**
     * An expression of a Java method call.
     *
     * Can be a static or an instance method. In the case of the former, the scope is a class name.
     * In the case of the latter — an object reference.
     *
     * @param T The method returned type.
     *
     * @param scope The scope of the method invocation: an instance receiving the method call, or
     *   the name of the class declaring a static method.
     * @param name The name of the method.
     * @param argument The argument passed to the method.
     * @param generics The list of the type arguments passed to the method.
     */
    public constructor(
        scope: JavaElement,
        name: String,
        argument: Expression<*>,
        generics: List<JavaTypeName> = listOf()
    ) : this(scope, name, listOf(argument), generics)

    /**
     * Constructs an expression of calling another method on the result of this method call.
     */
    @JvmOverloads
    public fun <R> chain(method: String, arguments: List<Expression<*>> = listOf()): MethodCall<R> =
        MethodCall(this, method, arguments)

    /**
     * Constructs an expression of calling another method on the result of this method call.
     */
    public fun <R> chain(method: String, vararg argument: Expression<*>): MethodCall<R> =
        MethodCall(this, method, argument.toList())

    /**
     * Constructs an expression chaining a call of the `build()` method.
     */
    public fun <R> chainBuild(): MethodCall<R> = chain("build")

    /**
     * Constructs an expression chaining a setter call.
     */
    public fun chainSet(field: String, value: Expression<*>): MethodCall<Message.Builder> =
        fieldAccess(field).setter(value)

    /**
     * Constructs an expression chaining a call of an `addField(...)` method.
     */
    public fun chainAdd(field: String, value: Expression<*>): MethodCall<Message.Builder> =
        fieldAccess(field).add(value)

    /**
     * Constructs an expression chaining a call of an `addAllField(...)` method.
     *
     * @see listExpression
     */
    public fun chainAddAll(field: String, value: Expression<*>): MethodCall<Message.Builder> =
        fieldAccess(field).addAll(value)

    /**
     * Constructs an expression chaining a call of a `putField(...)` method.
     */
    public fun chainPut(
        field: String,
        key: Expression<*>,
        value: Expression<*>
    ): MethodCall<Message.Builder> =
        fieldAccess(field).put(key, value)

    /**
     * Constructs an expression chaining a call of a `putAllField(...)` method.
     *
     * @see mapExpression
     */
    public fun chainPutAll(field: String, value: Expression<*>): MethodCall<Message.Builder> =
        fieldAccess(field).putAll(value)

    private fun fieldAccess(fieldName: String) = FieldAccess(Expression(toCode()), fieldName)
}

/**
 * Formats these class names as type arguments, including the angle brackets.
 */
private fun List<JavaTypeName>.genericTypes() =
    if (isEmpty()) "" else "<${joinToString()}>"

/**
 * Formats these expressions as method parameters, not including the brackets.
 */
private fun List<Expression<*>>.formatParams() =
    joinToString { it.toCode() }
