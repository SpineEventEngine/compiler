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

package io.spine.tools.compiler.test.uuid;

import com.google.common.base.Objects;
import io.spine.text.TextCoordinates;
import io.spine.tools.compiler.ast.TypeName;
import io.spine.tools.compiler.render.NonRepeatingInsertionPoint;
import io.spine.text.Text;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.compiler.ast.TypeNames.getQualifiedName;
import static io.spine.tools.compiler.render.CoordinatesFactory.nowhere;
import static java.lang.String.format;
import static kotlin.text.StringsKt.lines;

/**
 * An {@link InsertionPoint} in the scope of a generated Java class.
 *
 * <p>New member declarations should go under this insertion point.
 */
final class ClassScope implements NonRepeatingInsertionPoint {

    private static final String NATIVE_INSERTION_POINT_FMT =
            "// @@protoc_insertion_point(class_scope:%s)";

    private final TypeName typeName;

    ClassScope(TypeName name) {
        typeName = checkNotNull(name);
    }

    @Override
    public String getLabel() {
        return format("class_scope:%s", typeName.getTypeUrl());
    }

    /**
     * Finds the place to put the {@code ClassScope} insertion point among the given code lines.
     *
     * <p>To locate our insertion point, we use Protoc native {@code class_scope} insertion point.
     *
     * <p>If there is no Protoc native insertion point to be found, the {@code ClassScope} point
     * is not added either.
     */
    @Override
    public TextCoordinates locateOccurrence(String text) {
        String pattern = format(NATIVE_INSERTION_POINT_FMT, getQualifiedName(typeName));
        List<String> lines = lines(text);
        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (line.contains(pattern)) {
                return atLine(lineNumber);
            }
        }
        return nowhere();
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassScope scope = (ClassScope) o;
        return Objects.equal(typeName, scope.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(typeName);
    }
}
