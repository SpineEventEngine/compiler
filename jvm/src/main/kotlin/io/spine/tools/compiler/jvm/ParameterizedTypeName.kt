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

/**
 * A parameterized Java type name.
 *
 * The type name can be parameterized with any other [JavaTypeName].
 * Usually, it is class or a generic type variable (like `T` or `E`).
 *
 * Example usages:
 *
 * ```
 * val list = ClassName(List::class)
 * val string = ClassName(String::class)
 * val listOfStrings = ParameterizedTypeName(list, string)
 * println(listOfStrings) // java.util.List<java.lang.String>
 *
 * val map = ClassName(Map::class)
 * val genericMap = ParameterizedTypeName(map, TypeVariableName.T, TypeVariableName.E)
 * println(genericMap) // java.util.Map<T, E>
 *
 * val comparator = ClassName(Comparator::class)
 * val comparatorOfLists = ParameterizedTypeName(comparator, listOfStrings)
 * println(comparatorOfLists) // java.util.Comparator<java.util.List<java.lang.String>>
 *
 * val comparatorOfGenericMaps = ParameterizedTypeName(comparator, genericMap)
 * println(comparatorOfGenericMaps) // java.util.Comparator<java.util.Map<T, E>>
 * ```
 *
 * @param base The type name to parametrize.
 * @param parameters The type parameters to use.
 */
public class ParameterizedTypeName(
    base: JavaTypeName,
    parameters: List<JavaTypeName>
) : JavaTypeName() {

    init {
        require(parameters.isNotEmpty()) {
            "`${this::class.simpleName}` requires at least one type parameter to be passed."
        }
        require(parameters.all { it !in KnownPrimitives }) {
            "`${this::class.simpleName}` can't accept a primitive type as a generic parameter."
        }
    }

    public constructor(base: JavaTypeName, vararg parameter: JavaTypeName) : this(
        base,
        parameter.toList()
    )

    override val canonical: String = "$base<${parameters.joinToString()}>"
}
