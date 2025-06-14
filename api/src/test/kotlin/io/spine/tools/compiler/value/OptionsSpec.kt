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

package io.spine.tools.compiler.value

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.base.fieldPath
import io.spine.tools.compiler.ast.find
import io.spine.tools.compiler.given.value.DiceRoll
import io.spine.tools.compiler.given.value.FieldOptionSamplesProto
import io.spine.tools.compiler.given.value.KelvinTemperature
import io.spine.tools.compiler.given.value.NumberGenerated
import io.spine.tools.compiler.given.value.Range
import io.spine.tools.compiler.protobuf.ProtoFileList
import io.spine.tools.compiler.protobuf.toField
import io.spine.tools.compiler.protobuf.toPbSourceFile
import io.spine.tools.compiler.type.TypeSystem
import io.spine.option.MaxOption
import io.spine.option.MinOption
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Extensions for option types should")
internal class OptionsSpec {

    private val typeSystem: TypeSystem by lazy {
        val descriptors = setOf(FieldOptionSamplesProto.getDescriptor())
        val protoFiles = descriptors.map { java.io.File(it.file.name) }
        val protoSources = descriptors.map { it.toPbSourceFile() }.toSet()
        TypeSystem(ProtoFileList(protoFiles), protoSources)
    }

    @Test
    fun `parse integer values`() {
        val field = DiceRoll.getDescriptor().fields[0].toField()
        val minOption = field.optionList.find<MinOption>()
        val maxOption = field.optionList.find<MaxOption>()

        minOption shouldNotBe null
        maxOption shouldNotBe null

        minOption!!.parse(field, typeSystem) shouldBe value {
            intValue = 1
        }
        maxOption!!.parse(field, typeSystem) shouldBe value {
            intValue = 6
        }
    }

    @Test
    fun `parse floating point values`() {
        val field = KelvinTemperature.getDescriptor().fields[0].toField()
        val option = field.optionList.find<MinOption>()

        option shouldNotBe null
        option!!.parse(field, typeSystem) shouldBe value {
            doubleValue = 0.0
        }
    }

    @Test
    fun `parse reference in the same type`() {
        val field = Range.getDescriptor().fields[0].toField()
        val option = field.optionList.find<MaxOption>()

        option shouldNotBe null
        option!!.parse(field, typeSystem) shouldBe value {
            reference = reference {
                type = field.type
                target = fieldPath {
                    fieldName.add("max_value")
                }
            }
        }
    }

    @Test
    fun `parse references to nested fields`() {
        val field = NumberGenerated.getDescriptor().fields[0].toField()
        val minOption = field.optionList.find<MinOption>()
        val maxOption = field.optionList.find<MaxOption>()

        minOption shouldNotBe null
        maxOption shouldNotBe null

        minOption!!.parse(field, typeSystem) shouldBe value {
            reference = reference {
                type = field.type
                target = fieldPath {
                    fieldName.add("range")
                    fieldName.add("min_value")
                }
            }
        }

        maxOption!!.parse(field, typeSystem) shouldBe value {
            reference = reference {
                type = field.type
                target = fieldPath {
                    fieldName.add("range")
                    fieldName.add("max_value")
                }
            }
        }
    }

    @Test
    fun `pack options using correct type URL`() {
        val field = DiceRoll.getDescriptor().fields[0].toField()

        val min = field.optionList.find("min")!!
        min.value.typeUrl shouldBe "type.spine.io/MinOption"

        val max = field.optionList.find("max")!!
        max.value.typeUrl shouldBe "type.spine.io/MaxOption"
    }
}
