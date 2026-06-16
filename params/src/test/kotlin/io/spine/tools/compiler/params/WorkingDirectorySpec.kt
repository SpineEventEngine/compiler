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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`WorkingDirectory` should")
internal class WorkingDirectorySpec {

    @Test
    fun `create the directory if it does not exist`(@TempDir parent: Path) {
        val path = parent.resolve("working")
        path.exists() shouldBe false

        WorkingDirectory(path)

        path.isDirectory() shouldBe true
    }

    @Test
    fun `reject a path which is an existing file`(@TempDir parent: Path) {
        val file = parent.resolve("not-a-directory")
        file.writeText("I am a file, not a directory.")

        shouldThrow<IllegalArgumentException> {
            WorkingDirectory(file)
        }
    }

    @Test
    fun `provide the directory for parameter files`(@TempDir dir: Path) {
        WorkingDirectory(dir).parametersDirectory.path shouldBe dir.resolve("parameters")
    }

    @Test
    fun `provide the directory for settings files`(@TempDir dir: Path) {
        WorkingDirectory(dir).settingsDirectory.path shouldBe dir.resolve("settings")
    }

    @Test
    fun `provide the directory for request files`(@TempDir dir: Path) {
        WorkingDirectory(dir).requestDirectory.path shouldBe dir.resolve("requests")
    }

    @Test
    fun `expose its path`(@TempDir dir: Path) {
        WorkingDirectory(dir).path shouldBe dir
    }
}
