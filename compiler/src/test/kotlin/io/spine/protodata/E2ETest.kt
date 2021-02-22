/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.protodata

import com.github.ajalt.clikt.core.MissingOption
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.protodata.given.TestRenderer
import io.spine.protodata.given.TestSubscriber
import io.spine.protodata.test.DoctorProto
import io.spine.protodata.test.Journey
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.reflect.jvm.jvmName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class `Command-line app should` {

    private lateinit var srcRoot : Path
    private lateinit var codegenRequestFile: Path
    private lateinit var sourceFile: Path

    @BeforeEach
    fun prepareSources(@TempDir sandbox: Path) {
        srcRoot = sandbox.resolve("src")
        srcRoot.toFile().mkdirs()
        codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        sourceFile = srcRoot.resolve("SourceCode.java")
        sourceFile.writeText("""
            ${Journey::class.simpleName} worth taking
        """.trimIndent())

        val protoFile = DoctorProto.getDescriptor()
        val request = CodeGeneratorRequest
            .newBuilder()
            .addProtoFile(protoFile.toProto())
            .addFileToGenerate(protoFile.name)
            .build()
        codegenRequestFile.writeBytes(request.toByteArray())
    }

    @Test
    fun `render enhanced code`() {
        launchApp(
            "-s", TestSubscriber::class.jvmName,
            "-r", TestRenderer::class.jvmName,
            "--src", srcRoot.toString(),
            "-t", codegenRequestFile.toString()
        )
        assertThat(sourceFile.readText())
            .isEqualTo("\$Journey$ worth taking")
    }

    @Nested
    inner class `Fail if` {

        @Test
        fun `subscriber is missing`() {
            assertMissingOption {
                launchApp(
                    "-r", TestRenderer::class.jvmName,
                    "--src", srcRoot.toString(),
                    "-t", codegenRequestFile.toString()
                )
            }
        }

        @Test
        fun `renderer is missing`() {
            assertMissingOption {
                launchApp(
                    "-s", TestSubscriber::class.jvmName,
                    "--src", srcRoot.toString(),
                    "-t", codegenRequestFile.toString()
                )
            }
        }

        @Test
        fun `source root is missing`() {
            assertMissingOption {
                launchApp(
                    "-s", TestSubscriber::class.jvmName,
                    "-r", TestRenderer::class.jvmName,
                    "-t", codegenRequestFile.toString()
                )
            }
        }

        @Test
        fun `code generator request file is missing`() {
            assertMissingOption {
                launchApp(
                    "-s", TestSubscriber::class.jvmName,
                    "-r", TestRenderer::class.jvmName,
                    "--src", srcRoot.toString()
                )
            }
        }

        private fun assertMissingOption(block: () -> Unit) {
            Assertions.assertThrows(MissingOption::class.java, block)
        }
    }
}
