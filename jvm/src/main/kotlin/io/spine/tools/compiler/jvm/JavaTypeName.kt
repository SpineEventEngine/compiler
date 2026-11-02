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

import io.spine.tools.compiler.type.NameElement
import io.spine.tools.code.Java

/**
 * A name of a Java type.
 */
public abstract class JavaTypeName : NameElement<Java>, JavaElement {

    /**
     * The fully qualified name that uniquely identifies the type within
     * the Java language.
     *
     * May contain the following:
     *
     * 1. For classes and interfaces, this includes the package and any enclosing classes.
     * 2. For primitives, since they are not part of any package or class, the canonical
     * name is the same as the simple name.
     * 3. When the canonical name contains type variables, they are printed "as is".
     */
    public abstract val canonical: String

    override fun toCode(): String = canonical

    override fun toString(): String = toCode()

    override fun equals(other: Any?): Boolean =
        other is JavaTypeName && other.toCode() == toCode()

    override fun hashCode(): Int = toCode().hashCode()

    // Primitives were not extracted to a separate class for simplicity.
    // It is OK for us to treat them special cases of `JavaTypeName`.

    public object VOID : JavaTypeName() {
        override val canonical: String = "void"
    }
    public object BOOLEAN : JavaTypeName() {
        override val canonical: String = "boolean"
    }
    public object BYTE : JavaTypeName() {
        override val canonical: String = "byte"
    }
    public object SHORT : JavaTypeName() {
        override val canonical: String = "short"
    }
    public object INT : JavaTypeName() {
        override val canonical: String = "int"
    }
    public object LONG : JavaTypeName() {
        override val canonical: String = "long"
    }
    public object CHAR : JavaTypeName() {
        override val canonical: String = "char"
    }
    public object FLOAT : JavaTypeName() {
        override val canonical: String = "float"
    }
    public object DOUBLE : JavaTypeName() {
        override val canonical: String = "double"
    }

    public companion object {

        /**
         * The built-in Java primitive types.
         */
        @JvmStatic
        public val KnownPrimitives: List<JavaTypeName> by lazy {
            // Not computed in the static initializer of `JavaTypeName` because the objects
            // of primitive types may not be initialized yet. E.g., when `INT` is accessed
            // first, the JVM initializes `JavaTypeName` before assigning `INT.INSTANCE`.
            listOf(VOID, BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE)
        }
    }
}
