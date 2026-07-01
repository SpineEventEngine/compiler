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

package io.spine.tools.compiler.gradle.plugin

import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`File.residesIn` should")
internal class PathsSpec {

    @Test
    fun `treat a nested file as residing in the directory`(@TempDir dir: File) {
        val root = dir.resolve("test").apply { mkdirs() }
        val nested = root.resolve("java/Stub.java")

        nested.residesIn(root) shouldBe true
    }

    @Test
    fun `treat the directory itself as residing in it`(@TempDir dir: File) {
        val root = dir.resolve("test").apply { mkdirs() }

        root.residesIn(root) shouldBe true
    }

    @Test
    fun `not treat a sibling directory as residing in it`(@TempDir dir: File) {
        val test = dir.resolve("test").apply { mkdirs() }
        val sibling = dir.resolve("test2").apply { mkdirs() }

        sibling.residesIn(test) shouldBe false
    }

    @Test
    fun `accept a directory that is already canonical`(@TempDir dir: File) {
        val root = dir.resolve("test").apply { mkdirs() }
        val nested = root.resolve("Stub.java")

        nested.residesIn(root.canonicalFile.toPath()) shouldBe true
    }
}
