/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.tools.compiler.params

import io.kotest.matchers.shouldBe
import io.spine.format.Format
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.ast.toAbsoluteDirectory
import io.spine.tools.compiler.ast.toAbsoluteFile
import io.spine.type.toJson
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`ParametersDirectory` should")
internal class ParametersDirectorySpec {

    @Test
    fun `expose its path`(@TempDir dir: Path) {
        ParametersDirectory(dir).path shouldBe dir
    }

    @Test
    fun `compose the file name from the source set and default format`(@TempDir dir: Path) {
        val file = ParametersDirectory(dir).file(SourceSetName("main"))

        file.name shouldBe "main.pb.json"
        file.parentFile shouldBe dir.toFile()
    }

    @Test
    fun `use 'ProtoJson' as the default format`(@TempDir dir: Path) {
        val directory = ParametersDirectory(dir)
        val sourceSet = SourceSetName("main")

        directory.file(sourceSet) shouldBe directory.file(sourceSet, Format.ProtoJson)
    }

    @Test
    fun `compose the file name using the given format`(@TempDir dir: Path) {
        val file = ParametersDirectory(dir).file(SourceSetName("main"), Format.ProtoJson)

        file.name shouldBe "main.pb.json"
    }

    @Test
    fun `write the parameters to a file and return it`(@TempDir dir: Path) {
        val directory = ParametersDirectory(dir)
        val sourceSet = SourceSetName("main")
        val parameters = pipelineParameters {
            compiledProto.add(dir.resolve("compiled.proto").toAbsoluteFile())
            settings = dir.resolve("settings").toAbsoluteDirectory()
            request = dir.resolve("request.bin").toAbsoluteFile()
            targetRoot.add(dir.resolve("generated").toAbsoluteDirectory())
        }

        val written = directory.write(sourceSet, parameters)

        written shouldBe directory.file(sourceSet)
        written.exists() shouldBe true
        written.readText() shouldBe parameters.toJson()
    }
}
