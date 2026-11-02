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

@DisplayName("`TypeVariableName` Java API should")
class TypeVariableNameJavaSpec {

    @Test
    @DisplayName("expose common type variables via static `getT()` and `getE()`")
    void typeVariablesStatic() {
        assertThat(TypeVariableName.getT().getCanonical()).isEqualTo("T");
        assertThat(TypeVariableName.getE().getCanonical()).isEqualTo("E");
    }

    @Test
    @DisplayName("still expose common type variables via the companion object")
    void typeVariablesViaCompanion() {
        assertThat(TypeVariableName.Companion.getT()).isSameInstanceAs(TypeVariableName.getT());
        assertThat(TypeVariableName.Companion.getE()).isSameInstanceAs(TypeVariableName.getE());
    }
}
