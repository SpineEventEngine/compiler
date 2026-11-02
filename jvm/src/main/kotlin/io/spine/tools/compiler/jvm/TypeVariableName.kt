/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.compiler.jvm

/**
 * A type parameter name.
 *
 * Use it to declare a generic field or variable.
 *
 * An example usage:
 *
 * ```
 * val list = ClassName(List::class)
 * val genericList = ParameterizedClassName(list, TypeVariableName.E)
 * println(genericList) // `java.util.List<E>`
 * ```
 *
 * @see TypeVariableName.T
 * @see TypeVariableName.E
 */
public class TypeVariableName(value: String) : JavaTypeName() {

    override val canonical: String = value

    public companion object {

        /**
         * The type variable named `T`.
         */
        @JvmStatic
        public val T: TypeVariableName = TypeVariableName("T")

        /**
         * The type variable named `E`.
         */
        @JvmStatic
        public val E: TypeVariableName = TypeVariableName("E")
    }
}
