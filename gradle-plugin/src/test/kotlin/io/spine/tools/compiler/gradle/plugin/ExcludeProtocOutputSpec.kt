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

import com.google.protobuf.gradle.ProtobufPlugin
import io.kotest.matchers.shouldBe
import java.io.File
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("The plugin should exclude the `protoc` output from compilation")
internal class ExcludeProtocOutputSpec {

    /**
     * Reproduces the state in which the `protoc` output directory is among the
     * source directories of the `main` source set, and verifies that the file it
     * contains does not reach the `compileJava` task — see
     * [issue #19](https://github.com/SpineEventEngine/compiler/issues/19).
     */
    @Test
    fun `keeping it out of the Java compile task source`(@TempDir projectDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.group = "io.spine.stubs"
        with(project) {
            apply(plugin = "java")
            apply<ProtobufPlugin>()
            apply<Plugin>()
            repositories.mavenLocal()
        }

        // A file under the default `protoc` output directory, added to the source set
        // as if the source-set-level deduplication had not removed it.
        val protocJavaDir = projectDir.resolve("build/generated/sources/proto/main/java")
        protocJavaDir.mkdirs()
        val leaked = protocJavaDir.resolve("Leaked.java")
        leaked.writeText("class Leaked {}")
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        sourceSets.getByName("main").java.srcDir(protocJavaDir)

        (project as ProjectInternal).evaluate()

        val compileJava = project.tasks.getByName("compileJava") as JavaCompile
        compileJava.source.contains(leaked) shouldBe false
    }
}
