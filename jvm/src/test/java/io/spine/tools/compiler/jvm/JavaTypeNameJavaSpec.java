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

package io.spine.tools.compiler.jvm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`JavaTypeName` Java API should")
class JavaTypeNameJavaSpec {

    @Test
    @DisplayName("expose known primitives via static `getKnownPrimitives()`")
    void knownPrimitivesStatic() {
        assertThat(JavaTypeName.getKnownPrimitives())
                .containsExactly(
                        JavaTypeName.VOID.INSTANCE,
                        JavaTypeName.BOOLEAN.INSTANCE,
                        JavaTypeName.BYTE.INSTANCE,
                        JavaTypeName.SHORT.INSTANCE,
                        JavaTypeName.INT.INSTANCE,
                        JavaTypeName.LONG.INSTANCE,
                        JavaTypeName.CHAR.INSTANCE,
                        JavaTypeName.FLOAT.INSTANCE,
                        JavaTypeName.DOUBLE.INSTANCE
                ).inOrder();
    }

    @Test
    @DisplayName("still expose known primitives via the companion object")
    void knownPrimitivesViaCompanion() {
        assertThat(JavaTypeName.Companion.getKnownPrimitives())
                .isSameInstanceAs(JavaTypeName.getKnownPrimitives());
    }
}
