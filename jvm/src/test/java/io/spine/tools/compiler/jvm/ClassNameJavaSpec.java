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

@DisplayName("`ClassName` Java API should")
class ClassNameJavaSpec {

    @Test
    @DisplayName("guess a class name via static `guess()`")
    void guessStatic() {
        var className = ClassName.guess("io.spine.Outer.Nested");

        assertThat(className.getPackageName()).isEqualTo("io.spine");
        assertThat(className.getSimpleNames()).containsExactly("Outer", "Nested").inOrder();
    }

    @Test
    @DisplayName("still guess a class name via the companion object")
    void guessViaCompanion() {
        var name = "io.spine.Foo";

        assertThat(ClassName.Companion.guess(name)).isEqualTo(ClassName.guess(name));
    }

    @Test
    @DisplayName("expose the simple name regex via static `getSimpleNameRegex()`")
    void simpleNameRegexStatic() {
        var regex = ClassName.getSimpleNameRegex();

        assertThat(regex.matches("Foo")).isTrue();
        assertThat(regex.matches("1Foo")).isFalse();
        assertThat(regex).isSameInstanceAs(ClassName.Companion.getSimpleNameRegex());
    }

    @Test
    @DisplayName("expose separators as static constants")
    void separatorConstants() {
        assertThat(ClassName.BINARY_SEPARATOR).isEqualTo("$");
        assertThat(ClassName.CANONICAL_SEPARATOR).isEqualTo(".");
        assertThat(ClassName.PACKAGE_SEPARATOR).isEqualTo(".");
        assertThat(ClassName.PATH_SEPARATOR).isEqualTo("/");
    }
}
