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

import com.google.common.collect.ImmutableList;
import io.spine.tools.compiler.ast.File;
import io.spine.tools.compiler.ast.TypeName;
import io.spine.tools.compiler.jvm.ClassName;
import io.spine.tools.compiler.jvm.render.JavaRenderer;
import io.spine.tools.compiler.render.InsertionPoint;
import io.spine.tools.compiler.render.SourceFileSet;
import io.spine.tools.compiler.test.UuidType;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * A renderer which adds the {@code randomId()} factory methods to the UUID types.
 *
 * <p>A UUID type is a message which only has one field — a {@code string} field
 * called {@code uuid}.
 */
@SuppressWarnings("unused") // Accessed by the Compiler via refection.
public final class UuidJavaRenderer extends JavaRenderer {

    /**
     * The indentation level of one offset (four space characters).
     */
    private static final int INDENT_LEVEL = 1;

    private static final Template METHOD_FORMAT = Template.from(
            "public static %s randomId() {",
            "    return newBuilder().setUuid(",
                    "%s.randomUUID().toString()",
            "     ).build(); ",
            "}");

    /**
     * Renders the random ID factory method for all UUID types.
     *
     * <p>If a class represents a UUID type, places a public static method into the class scope.
     * The method generates a new instance of the class with a random UUID value.
     *
     * <p>A UUID type is a message with a single string field called UUID.
     */
    @Override
    protected void render(SourceFileSet sources) {
        Set<UuidType> uuidTypes = select(UuidType.class).all();
        for (UuidType type : uuidTypes) {
            var typeName = type.getName();
            var file = type.getDeclaredIn();
            var className = classNameOf(typeName, file);
            var classScope = new ClassScope(typeName);
            var lines = METHOD_FORMAT.format(className, UUID.class.getName());
            var javaFilePath = javaFileOf(typeName, file);

            // If there are no Java files, we deal with another language.
            // Have this workaround until we get access to the `sourceRoot` property.
            if (sources.findFile(javaFilePath).isEmpty()) {
                continue;
            }

            sources.file(javaFilePath)
                   .at(classScope)
                   .withExtraIndentation(INDENT_LEVEL)
                   .add(lines);
        }
    }
}
