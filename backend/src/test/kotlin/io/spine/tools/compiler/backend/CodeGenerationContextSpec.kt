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

package io.spine.tools.compiler.backend

import com.google.protobuf.AnyProto
import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.EmptyProto
import com.google.protobuf.StringValue
import com.google.protobuf.TimestampProto
import com.google.protobuf.WrappersProto
import com.google.protobuf.compiler.codeGeneratorRequest
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.collect.theOnly
import io.spine.option.OptionsProto
import io.spine.option.OptionsProto.BETA_TYPE_FIELD_NUMBER
import io.spine.protobuf.AnyPacker
import io.spine.time.TimeProto
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BOOL
import io.spine.tools.compiler.ast.ProtobufDependency
import io.spine.tools.compiler.ast.ProtobufSourceFile
import io.spine.tools.compiler.ast.doc
import io.spine.tools.compiler.ast.option
import io.spine.tools.compiler.ast.span
import io.spine.tools.compiler.ast.toAbsoluteFile
import io.spine.tools.compiler.ast.toType
import io.spine.tools.compiler.backend.event.CompilerEvents
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.protobuf.file
import io.spine.tools.compiler.test.DoctorProto
import io.spine.tools.compiler.test.PhDProto
import io.spine.tools.compiler.test.XtraOptsProto
import io.spine.tools.compiler.type.findAbsolute
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`Code Generation` context should")
class CodeGenerationContextSpec {

    @Test
    fun `stay apart from the context of another pipeline in one JVM`() {
        val doctorFile = typeSystem.findAbsolute(DoctorProto.getDescriptor())
            .shouldNotBeNull()
            .toAbsoluteFile()
        val first = CodeGenerationContext.newInstance()
        CodeGenerationContext.newInstance().use { second ->
            first.use {
                emitCompilerEvents(it)
                it.sourceFile(doctorFile).shouldNotBeNull()
                second.sourceFile(doctorFile).shouldBeNull()
            }

            // The other context keeps working after the first one is closed.
            second.isOpen shouldBe true
            emitCompilerEvents(second)
            second.sourceFile(doctorFile).shouldNotBeNull()
        }
    }

    @Test
    fun `close the third-party contexts of its pipeline`() {
        val ctx = CodeGenerationContext.newInstance()
        val thirdParties = listOf(
            ctx.insertionPointsContext,
            ctx.protobufCompiler.thirdParty,
            ctx.configuration.thirdParty,
        )
        thirdParties.forEach { it.isOpen shouldBe true }

        ctx.close()

        ctx.isOpen shouldBe false
        ctx.context.isOpen shouldBe false
        thirdParties.forEach { it.isOpen shouldBe false }
    }

    @Nested
    @DisplayName("provide")
    inner class ViewTypes {

        private lateinit var ctx: CodegenContext

        @BeforeEach
        fun setUp() {
            ctx = CodeGenerationContext.newInstance()
        }

        @AfterEach
        fun closeContext() {
            ctx.close()
        }

        @Test
        fun `'ProtobufSourceFile' view`() = assertTrue(
            ctx.hasEntitiesOfType(ProtoSourceFileView::class.java)
        )

        @Test
        fun `'ProtobufDependency' file view`() = assertTrue(
            ctx.hasEntitiesOfType(DependencyView::class.java)
        )
    }

    companion object Fixtures {

        val dependencies = listOf(
            AnyProto.getDescriptor(),
            DescriptorProtos.getDescriptor(),
            DoctorProto.getDescriptor(),
            EmptyProto.getDescriptor(),
            OptionsProto.getDescriptor(),
            PhDProto.getDescriptor(),
            TimestampProto.getDescriptor(),
            TimeProto.getDescriptor(),
            WrappersProto.getDescriptor(),
            XtraOptsProto.getDescriptor(),
        ).map { it.toProto() }

        val filesToGenerate = setOf(
            DoctorProto.getDescriptor().name,
            PhDProto.getDescriptor().name
        )

        private val codeGeneratorRequest = codeGeneratorRequest {
            protoFile.addAll(dependencies)
            fileToGenerate.addAll(filesToGenerate)
        }

        private val typeSystem by lazy {
            createTypeSystem(codeGeneratorRequest)
        }

        /**
         * Emits the events of the Protobuf compiler, parsed from the [codeGeneratorRequest],
         * into the given context.
         */
        fun emitCompilerEvents(context: CodeGenerationContext) {
            val events = CompilerEvents.parse(codeGeneratorRequest, typeSystem, { true })
            context.protobufCompiler.emitted(events)
        }
    }

    @Nested
    inner class `construct views based on a descriptor set` {

        private lateinit var ctx: CodeGenerationContext

        @BeforeEach
        fun buildViews() {
            ctx = CodeGenerationContext.newInstance()
            emitCompilerEvents(ctx)
        }

        @AfterEach
        fun closeContext() {
            ctx.close()
        }

        @Test
        fun `with files marked for generation`() {
            val fullPath = typeSystem.findAbsolute(DoctorProto.getDescriptor()).shouldNotBeNull()
            val actual = ctx.select(ProtobufSourceFile::class.java)
                .findById(fullPath.toAbsoluteFile())
                .shouldNotBeNull()

            val types = actual.typeMap
            val typeName = "type.spine.io/spine.compiler.test.Journey"
            types shouldContainKey typeName
            val journeyType = types[typeName]!!
            journeyType.name.typeUrl shouldBe typeName
            journeyType.optionList should containExactly(option {
                name = "beta_type"
                number = BETA_TYPE_FIELD_NUMBER
                type = TYPE_BOOL.toType()
                value = AnyPacker.pack(BoolValue.of(true))
                doc = doc {
                    // The option is not documented.
                    // It's OK because we test docs of options in the tests of the `api` module.
                }
                span = span {
                    startLine = 34
                    startColumn = 5
                    endLine = 34
                    endColumn = 31
                }
            })
            // 4 regular fields and 2 fields under `oneof`.
            journeyType.fieldList shouldHaveSize 6
            journeyType.oneofGroupList shouldHaveSize 1
            journeyType.oneofGroupList[0].fieldList shouldHaveSize 2
        }

        @Test
        fun `with dependencies`() {
            ctx.select(ProtobufDependency::class.java)
                .findById(AnyProto.getDescriptor().file())
                .shouldNotBeNull()
        }

        @Test
        fun `with respect for custom options among the source files`() {
            val filePath = typeSystem.findAbsolute(PhDProto.getDescriptor()).shouldNotBeNull()
            val phdFile = ctx.select(ProtobufSourceFile::class.java)
                .findById(filePath.toAbsoluteFile())
                .shouldNotBeNull()
            val paperType = phdFile.typeMap.values.find { it.name.simpleName == "Paper" }!!
            val keywordsField = paperType.fieldList.find { it.name.value == "keywords" }!!
            keywordsField.optionList shouldHaveSize 1
            val option = keywordsField.optionList.theOnly()
            option.name shouldBe "xtra_option"
            option.value.unpack(StringValue::class.java).value shouldContain "please"
        }
    }
}

/**
 * Obtains the view on the Protobuf source file with the given path, if this context has one.
 */
private fun CodegenContext.sourceFile(file: File): ProtobufSourceFile? =
    select(ProtobufSourceFile::class.java).findById(file)
