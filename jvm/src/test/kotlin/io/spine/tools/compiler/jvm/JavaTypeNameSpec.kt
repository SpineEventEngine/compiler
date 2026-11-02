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

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.tools.compiler.jvm.given.PrimitiveFirstProgram
import io.spine.tools.compiler.jvm.given.runInFreshJvm
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`JavaTypeName` should")
internal class JavaTypeNameSpec {

    private val myCanonical = "my-canonical-name"
    private val myType = typeName(myCanonical)

    @Test
    fun `return the provided canonical name`() {
        myType.canonical shouldBe myCanonical
    }

    @Test
    fun `return the canonical name as string representation`() {
        "$myType" shouldBe myCanonical
    }

    @Test
    fun `compare two elements`() {
        val myType2 = typeName(myCanonical)
        myType shouldBe myType2
        myType.hashCode() shouldBe myType2.hashCode()

        val anotherType = typeName("another-canonical-name")
        myType shouldNotBe anotherType
        myType.hashCode() shouldNotBe anotherType.hashCode()
    }

    @Test
    fun `list all known primitives when a primitive is initialized first`(
        @TempDir workDir: Path
    ) {
        val run = runInFreshJvm(PrimitiveFirstProgram::class.java, workDir)
        withClue(run.output) {
            run.exitCode shouldBe 0
        }
    }
}

private fun typeName(canonical: String) = object : JavaTypeName() {
    override val canonical: String = canonical
}
