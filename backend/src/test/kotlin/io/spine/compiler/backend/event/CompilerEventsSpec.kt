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

package io.spine.compiler.backend.event

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos.FileOptions
import com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS
import com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS_VALUE
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.codeGeneratorRequest
import com.google.protobuf.enumValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.base.EventMessage
import io.spine.compiler.ast.event.FieldOptionDiscovered
import io.spine.compiler.ast.event.FileEntered
import io.spine.compiler.ast.event.FileOptionDiscovered
import io.spine.compiler.ast.event.RpcOptionDiscovered
import io.spine.compiler.ast.event.TypeDiscovered
import io.spine.compiler.ast.messageType
import io.spine.compiler.ast.toJava
import io.spine.compiler.ast.typeName
import io.spine.compiler.backend.createTypeSystem
import io.spine.compiler.test.DoctorProto
import io.spine.option.OptionsProto
import io.spine.protobuf.unpackGuessingType
import io.spine.testing.Correspondences
import io.spine.type.KnownTypes
import kotlin.reflect.KClass
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the [CompilerEvents] class basing on the `doctor.proto`.
 *
 * See the file at `test-env/src/main/proto/spine/protodata/test/doctor.proto`.
 */
@DisplayName("`CompilerEvents` should")
class CompilerEventsSpec {

    companion object {

        private val nl: String = System.lineSeparator()

        private lateinit var events: List<EventMessage>

        @BeforeAll
        @JvmStatic
        fun parseEvents() {
            val request = createRequest()
            val typeSystem = createTypeSystem(request)
            events = CompilerEvents.parse(request, typeSystem, { true }).toList()
        }

        private fun createRequest(): CodeGeneratorRequest {
            // Gather files of all known types to simplify resolving dependencies.
            val allDependencyFiles = KnownTypes.instance()
                .asTypeSet()
                .messageTypes()
                .map { it.descriptor().file.toProto() }

            val request = codeGeneratorRequest {
                fileToGenerate += DoctorProto.getDescriptor().fullName
                protoFile.addAll(allDependencyFiles)
            }
            return request
        }
    }

    @Test
    fun `not have duplicate events`() {
        events.distinct() shouldContainExactly events
    }

    @Nested
    inner class `produce file events` {

        @Test
        fun `events in order`() = assertEmits(
            FileEntered::class, // Starts with the file.
            FileOptionDiscovered::class, // Now come options.
            FileOptionDiscovered::class,
            FileOptionDiscovered::class,
            FileOptionDiscovered::class,
            io.spine.compiler.ast.event.FileExited::class // Ends with the file.
        )

        @Test
        fun `for standard file option`(): Unit = with(events) {
            findJavaPackageEvent().option<StringValue>().value shouldBe "io.spine.compiler.test"
            findOuterClassNameEvent().option<StringValue>().value shouldBe "DoctorProto"
            findMultipleFilesEvent().option<BoolValue>().value shouldBe true
        }

        @Test
        fun `for custom file option`() {
            val event = events.findTypeUrlPrefixEvent()

            event shouldNotBe null
            event.option<StringValue>().value shouldBe "type.spine.io"
        }

        @Test
        fun `with full file path`() {
            events.first { it is FileEntered }.let { event ->
                (event as FileEntered).file.toJava().isAbsolute shouldBe true
            }
        }
    }

    @Nested
    inner class `produce type events` {

        @Test
        fun `in order`() = assertEmits(
            FileEntered::class,

            io.spine.compiler.ast.event.TypeEntered::class,
            io.spine.compiler.ast.event.TypeExited::class,

            io.spine.compiler.ast.event.FileExited::class
        )

        @Test
        fun `matching nesting of types`() = assertEmits(
            io.spine.compiler.ast.event.TypeEntered::class,
            io.spine.compiler.ast.event.TypeEntered::class,
            io.spine.compiler.ast.event.TypeExited::class,
            io.spine.compiler.ast.event.TypeExited::class
        )
    }

    @Nested
    inner class `produce field events` {

        @Test
        fun `in order`() = assertEmits(
            FileEntered::class,
            io.spine.compiler.ast.event.TypeEntered::class,

            io.spine.compiler.ast.event.FieldEntered::class,
            FieldOptionDiscovered::class,
            io.spine.compiler.ast.event.FieldExited::class,

            io.spine.compiler.ast.event.TypeExited::class,
            io.spine.compiler.ast.event.FileExited::class
        )

        @Test
        fun `for custom field option`() {
            val event = events.findRequiredFieldOptionEvent()

            event shouldNotBe null
            event.option<BoolValue>().value shouldBe true
        }
    }

    @Test
    fun `produce 'oneof' events`() = assertEmits(
        FileEntered::class,
        io.spine.compiler.ast.event.TypeEntered::class,

        io.spine.compiler.ast.event.OneofGroupEntered::class,
        io.spine.compiler.ast.event.FieldEntered::class,
        FieldOptionDiscovered::class,
        io.spine.compiler.ast.event.FieldExited::class,
        io.spine.compiler.ast.event.OneofGroupExited::class,

        io.spine.compiler.ast.event.TypeExited::class,
        io.spine.compiler.ast.event.FileExited::class
    )

    @Test
    fun `produce enum events`() = assertEmits(
        FileEntered::class,
        io.spine.compiler.ast.event.EnumEntered::class,
        io.spine.compiler.ast.event.EnumConstantEntered::class,
        io.spine.compiler.ast.event.EnumConstantExited::class,
        io.spine.compiler.ast.event.EnumConstantEntered::class,
        io.spine.compiler.ast.event.EnumConstantExited::class,
        io.spine.compiler.ast.event.EnumConstantEntered::class,
        io.spine.compiler.ast.event.EnumConstantExited::class,
        io.spine.compiler.ast.event.EnumExited::class
    )

    @Test
    fun `produce service events`() = assertEmits(
        io.spine.compiler.ast.event.ServiceEntered::class,
        io.spine.compiler.ast.event.RpcEntered::class,
        RpcOptionDiscovered::class,
        io.spine.compiler.ast.event.RpcExited::class,
        io.spine.compiler.ast.event.ServiceExited::class
    )

    @Test
    fun `include 'rpc' options`() {
        val event = emitted<RpcOptionDiscovered>()

        event.option.name shouldBe "idempotency_level"
        event.option.value.unpackGuessingType() shouldBe enumValue {
            name = NO_SIDE_EFFECTS.name
            number = NO_SIDE_EFFECTS_VALUE
        }
    }

    @Test
    fun `include message doc info`() {
        val typeEntered = emitted<TypeDiscovered>()
        assertThat(typeEntered.type)
            .comparingExpectedFieldsOnly()
            .isEqualTo(messageType {
                name = typeName { simpleName = "Journey" }
                file = typeEntered.type.file
            })

        val doc = typeEntered.type.doc
        doc.leadingComment.split(nl) shouldContainExactly listOf(
            "A Doctor's journey.",
            "",
            "A test type",
            ""
        )

        doc.trailingComment shouldBe "Impl note: test type."
        doc.detachedCommentList[0] shouldBe "Detached 1."

        doc.detachedCommentList[1].split(nl) shouldContainExactly listOf(
            "Detached 2.",
            "Indentation is not preserved in Protobuf.",
            "",
            "Bla bla!"
        )
    }

    @Test
    fun `parse repeated values of custom options`() = assertEmits(
        io.spine.compiler.ast.event.TypeEntered::class, // message Word
        io.spine.compiler.ast.event.FieldEntered::class, // string test
        FieldOptionDiscovered::class, // (rhyme)
        FieldOptionDiscovered::class, // (rhyme)
        FieldOptionDiscovered::class, // (rhyme)
        io.spine.compiler.ast.event.FieldExited::class,
        io.spine.compiler.ast.event.TypeExited::class,
    )

    private fun assertEmits(vararg types: KClass<out EventMessage>) {
        val javaClasses = types.map { it.java }
        assertThat(events)
            .comparingElementsUsing(Correspondences.type<EventMessage>())
            .containsAtLeastElementsIn(javaClasses)
            .inOrder()
    }

    private inline fun <reified E : EventMessage> emitted(): E {
        val javaClass = E::class.java
        return events.find { it.javaClass == javaClass }!! as E
    }
}

/**
 * Obtains the option of the given type [T] from this [FileOptionDiscovered] event.
 *
 * The receiver type is nullable for brevity of the calls after `isNotNull()`.
 */
private inline fun <reified T : Message> FileOptionDiscovered?.option() : T {
    return this!!.option.value.unpack(T::class.java)
}

/**
 * Obtains the option of the given type [T] from this [FieldOptionDiscovered] event.
 *
 * The receiver type is nullable for brevity of the calls after `isNotNull()`.
 */
private inline fun <reified T : Message> FieldOptionDiscovered?.option() : T {
    return this!!.option.value.unpack(T::class.java)
}

private fun List<EventMessage>.findTypeUrlPrefixEvent(): FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.option.number == OptionsProto.TYPE_URL_PREFIX_FIELD_NUMBER
} as FileOptionDiscovered?

private fun List<EventMessage>.findJavaPackageEvent(): FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.option.number == FileOptions.JAVA_PACKAGE_FIELD_NUMBER
}  as FileOptionDiscovered?

private fun List<EventMessage>.findOuterClassNameEvent(): FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.option.number == FileOptions.JAVA_OUTER_CLASSNAME_FIELD_NUMBER
}  as FileOptionDiscovered?

private fun List<EventMessage>.findMultipleFilesEvent() : FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.isJavaMultipleFilesField()
} as FileOptionDiscovered?

private fun FileOptionDiscovered.isJavaMultipleFilesField() =
    option.number == FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER

private fun List<EventMessage>.findRequiredFieldOptionEvent(): FieldOptionDiscovered? = find {
    it is FieldOptionDiscovered && it.option.number == OptionsProto.REQUIRED_FIELD_NUMBER
} as FieldOptionDiscovered?
