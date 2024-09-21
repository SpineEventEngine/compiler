/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.test.annotation;

import io.spine.protodata.java.render.JavaRenderer;
import io.spine.protodata.render.SourceFileSet;
import io.spine.protodata.test.Annotated;
import io.spine.protodata.test.FieldId;

import java.nio.file.Path;
import java.util.Set;

/**
 * Renders Java annotations on field getters for fields marked with
 * the {@code (java_annotation)} option.
 */
@SuppressWarnings("unused") // Accessed reflectively by ProtoData.
public final class AnnotationRenderer extends JavaRenderer {

    private static final int INDENT_LEVEL = 1;

    @Override
    protected void render(SourceFileSet sources) {
        // Don't do anything if this source file set is for a language
        // other than Java. For the root cause of this please see this issue:
        // https://github.com/SpineEventEngine/ProtoData/issues/90
         if (!sources.inputRoot().endsWith("java")) {
             return;
         }
        var annotatedFields = select(Annotated.class).all();
        annotatedFields.forEach(field -> renderFor(field, sources));

        sources.forEach(file -> {
           file.at(new MessageClass())
               .withExtraIndentation(2)
               .add("@javax.annotation.Generated(" + System.lineSeparator() +
                    "    \"by ProtoData tests\"" + System.lineSeparator() +
                    ")");
        });
    }

    private void renderFor(Annotated field, SourceFileSet sourceSet) {
        var id = field.getId();
        var getter = new FieldGetter(id);
        var path = javaFileOf(id.getType(), id.getFile());
        sourceSet.file(path)
                 .at(getter)
                 .withExtraIndentation(INDENT_LEVEL)
                 .add('@' + field.getJavaAnnotation());
    }
}
