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

import io.spine.tools.compiler.ast.Cardinality
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_LIST
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_MAP
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.FieldName
import io.spine.tools.compiler.ast.camelCase
import io.spine.tools.compiler.ast.cardinality
import io.spine.tools.compiler.ast.fieldName
import io.spine.string.camelCase

/**
 * Abstract base for field access conventions.
 */
public abstract class FieldConventions(
    protected val name: FieldName,
    protected val kind: Cardinality
) {
    protected val getterName: String
        get() = when (kind) {
            CARDINALITY_LIST -> getListName
            CARDINALITY_MAP -> getMapName
            else -> prefixed("get")
        }

    private val getListName: String
        get() = "get${name.camelCase}List"

    private val getMapName: String
        get() = "get${name.camelCase}Map"

    protected val setterName: String
        get() = when (kind) {
            CARDINALITY_LIST -> addAllName
            CARDINALITY_MAP -> putAllName
            else -> prefixed("set")
        }

    protected val addName: String
        get() = prefixed("add")

    protected val addAllName: String
        get() = prefixed("addAll")

    protected val putName: String
        get() = prefixed("put")

    protected val putAllName: String
        get() = prefixed("putAll")

    private fun prefixed(prefix: String): String =
        "$prefix${name.value.camelCase()}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldConventions) return false

        if (name != other.name) return false
        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

/**
 * Obtains the names of the methods associated with the given field.
 *
 * The class is made `open` for accessing `protected` properties of
 * [FieldConventions] via inheriting this class.
 */
public open class FieldMethods(
    name: FieldName,
    kind: Cardinality
) : FieldConventions(name, kind) {

    public constructor(field: Field): this(field.name, field.type.cardinality)

    /**
     * The name of the primary method that sets a value of the field.
    */
    public val primarySetter: String = setterName

    /**
     * The name of the accessor method for the field.
     */
    public val getter: String = getterName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldMethods) return false
        if (!super.equals(other)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    public companion object {

        /**
         * Obtains the name of the getter.
         */
        @JvmStatic
        public fun getterOf(fieldName: String, cardinality: Cardinality): String {
            return FieldMethods(fieldName { value = fieldName }, cardinality).getter
        }
    }
}
