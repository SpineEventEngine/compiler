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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * A declaration of a Java field.
 *
 * The declared field may OR may not be initialized.
 */
public class FieldDeclaration<T>(
    public val name: String,
    code: String
) : MemberDeclaration(code) {

    /**
     * Declares an initialized Java field.
     *
     * An example usage:
     *
     * ```
     * val height = FieldDeclaration("public final", PrimitiveName.INT, "height", Expression<Int>("180"))
     * println(height) // `public final int height = 180;`
     * ```
     *
     * @param modifiers The field modifiers separated with a space.
     * @param type The field type.
     * @param name The field name.
     * @param value The field value.
     */
    public constructor(
        modifiers: String,
        type: JavaTypeName,
        name: String,
        value: Expression<T>
    ) : this(name, "$modifiers $type $name = $value;")

    /**
     * Declares a non-initialized Java field.
     *
     * An example usage:
     *
     * ```
     * val height = FieldDeclaration("public final", PrimitiveName.INT, "height")
     * println(height) // `public final int height;`
     * ```
     *
     * @param modifiers The field modifiers separated with a space.
     * @param type The field type.
     * @param name The field name.
     */
    public constructor(
        modifiers: String,
        type: JavaTypeName,
        name: String
    ) : this(name, "$modifiers $type $name;")

    /**
     * Returns an expression that reads the value of this field.
     */
    public fun read(useThis: Boolean = false): ReadField<T> = ReadField(name, useThis)
}

/**
 * Provides a read access to the field with the given name.
 *
 * An example usage:
 *
 * ```
 * val user = ReadField<User>("user", useThis = true)
 * println(user) // `this.user`
 * ```
 *
 * @param T The type of the field.
 * @param name The name of the field.
 * @param explicitThis Tells whether to use the explicit `this` keyword.
 */
public class ReadField<T>(name: String, explicitThis: Boolean = false) :
    Expression<T>(field(name, explicitThis))

/**
 * Creates a new [PsiField] from this Java [FieldDeclaration].
 */
public fun FieldDeclaration<*>.toPsi(context: PsiElement? = null): PsiField =
    elementFactory.createFieldFromText(toCode(), context)

private fun field(name: String, useThis: Boolean) = if (useThis) "this.$name" else name
