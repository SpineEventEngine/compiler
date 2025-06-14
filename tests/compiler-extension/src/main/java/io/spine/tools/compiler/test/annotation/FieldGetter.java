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

package io.spine.tools.compiler.test.annotation;

import io.spine.text.TextCoordinates;
import io.spine.tools.compiler.render.NonRepeatingInsertionPoint;
import io.spine.tools.compiler.test.FieldId;
import io.spine.text.Text;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.string.Strings.camelCase;
import static java.lang.String.format;
import static io.spine.tools.compiler.render.CoordinatesFactory.nowhere;
import static kotlin.text.StringsKt.lines;

/**
 * An insertion point at the line right before a getter method of the given field.
 *
 * <p>This implementation should only be used for test purposes. It might not cover all the possible
 * edge cases when fining the line where the getter is.
 */
final class FieldGetter implements NonRepeatingInsertionPoint {

    private final FieldId field;

    FieldGetter(FieldId field) {
        this.field = checkNotNull(field);
    }

    @NonNull
    @Override
    public String getLabel() {
        return format("getter-for:%s.%s", field.getType().getTypeUrl(), field.getField().getValue());
    }

    @NonNull
    @Override
    public TextCoordinates locateOccurrence(String text) {
        var fieldName = camelCase(field.getField().getValue());
        var getterName = "get" + fieldName;
        var pattern = Pattern.compile("public .+ " + getterName);
        var lines = lines(text);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (pattern.matcher(line).find()) {
                return atLine(i);
            }
        }
        return nowhere();
    }
}
