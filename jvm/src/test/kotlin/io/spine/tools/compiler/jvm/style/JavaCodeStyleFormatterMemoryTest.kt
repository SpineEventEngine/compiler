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

package io.spine.tools.compiler.jvm.style

import copyResource
import io.kotest.matchers.string.shouldContain
import io.spine.tools.compiler.backend.Pipeline
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.compiler.settings.SettingsDirectory
import io.spine.tools.compiler.style.indentOptions
import io.spine.testing.compiler.parametersWithSettingsDir
import io.spine.testing.compiler.pipelineParams
import io.spine.testing.compiler.withRoots
import io.spine.testing.compiler.withSettingsDir
import io.spine.format.Format
import io.spine.type.toJson
import java.nio.file.Files.readString
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * This test ensures that bigger Java files are handled by [JavaCodeStyleFormatterPlugin].
 */
internal class JavaCodeStyleFormatterMemoryTest {

    @Test
    fun `use custom indentation settings`() {
        formattedCode shouldContain INDENT + "public static void registerAllExtensions("
        formattedCode shouldContain
                CONT_INDENT + "com.google.protobuf.ExtensionRegistryLite registry) {"
    }

    companion object {

        /**
         * Set the indentation size other than used in IntelliJ Platform
         * settings for Java by default, so that we see that a custom value works.
         */
        private const val INDENT_SIZE = 7
        private const val CONT_INDENT_SIZE = 13

        val INDENT = " ".repeat(INDENT_SIZE)
        val CONT_INDENT = " ".repeat(CONT_INDENT_SIZE)

        /**
         * The name of the resource file with the source code for testing the formatting.
         */
        private const val fileName = "java/memory/MainRejections.java"

        private lateinit var outputDir: Path
        private lateinit var formattedCode: String

        @BeforeAll
        @JvmStatic
        fun runPipeline(
            @TempDir sandbox: Path,
            @TempDir inputDir: Path,
            @TempDir outputDir: Path
        ) {
            this.outputDir = outputDir
            val settingsDir = WorkingDirectory(sandbox).settingsDirectory
            writeSettings(settingsDir)
            val params = pipelineParams {
                withRoots(inputDir, outputDir)
                withSettingsDir(settingsDir.path)
            }
                parametersWithSettingsDir(settingsDir.path)
            copyResource(fileName, inputDir)

            Pipeline(
                params = params,
                plugin =  JavaCodeStyleFormatterPlugin(),
            )()

            formattedCode = readString(outputDir.resolve(fileName))
        }

        private fun writeSettings(settings: SettingsDirectory) {
            val javaStyle = javaCodeStyleDefaults().toBuilder().apply {
                indentOptions = indentOptions {
                    indentSize = INDENT_SIZE
                    continuationIndentSize = CONT_INDENT_SIZE
                }
            }

            settings.write(
                JavaCodeStyleFormatter.settingsId,
                Format.ProtoJson,
                javaStyle.toJson()
            )
        }
    }
}
