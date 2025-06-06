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

package io.spine.tools.compiler.jvm.annotation

import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import io.spine.annotation.Modified
import io.spine.base.Time
import io.spine.tools.compiler.Constants.CLI_APP_CLASS
import io.spine.tools.compiler.backend.Pipeline
import io.spine.tools.compiler.jvm.JAVA_FILE
import io.spine.tools.compiler.jvm.WithSourceFileSet
import io.spine.tools.compiler.jvm.annotation.ModifiedTypeAnnotation.Companion.currentDateTime
import io.spine.tools.compiler.render.SourceFile
import io.spine.testing.compiler.pipelineParams
import io.spine.testing.compiler.withRoots
import io.spine.time.testing.FrozenMadHatterParty
import io.spine.time.toTimestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.io.path.Path
import kotlin.io.path.readText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`ModifiedTypeAnnotation` renderer should")
internal class ModifiedTypeAnnotationSpec : WithSourceFileSet() {

    private val qualifiedAnnotation = Modified::class.java.canonicalName

    @Test
    fun `add the annotation, assuming 'CLI_APP_CLASS' as the default generator`() {
        runPipelineWith(ModifiedTypeAnnotation())
        assertGenerated(
            "@$qualifiedAnnotation(\"$CLI_APP_CLASS\")"
        )
    }

    @Test
    fun `use given generator value`() {
        runPipelineWith(ModifiedTypeAnnotation(javaClass.name))
        assertGenerated(
            "@$qualifiedAnnotation(\"${javaClass.name}\")"
        )
    }

    @Nested
    inner class TimestampTests {

        private var frozenTime: ZonedDateTime? = null

        @BeforeEach
        fun freezeTime() {
            // Have time shifted, event when testing at UTC.
            frozenTime = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"))
            val timeProvider = FrozenPartyAtTimezone(frozenTime!!)
            Time.setProvider(timeProvider)
        }

        @AfterEach
        fun unfreezeTime() {
            Time.resetProvider()
            frozenTime = null
        }

        @Test
        fun `produce timestamp of code generation`() {
            runPipelineWith(ModifiedTypeAnnotation(
                generator = javaClass.name,
                addTimestamp = true
            ))

            val expectedDate = currentDateTime()

            val expectedCode = """
                 @$qualifiedAnnotation(
                     value = "${javaClass.name}",
                     date = "$expectedDate"
                 )
                 """.trimIndent().replace("\n", System.lineSeparator())

            val assertCode = assertCode()
            assertCode.contains(expectedCode)
            assertCode.contains("+03:00\"") // Istanbul zone offset
        }
    }

    @Test
    fun `adds comments for the given file`() {
        val addFileName : (SourceFile<*>) -> String = {
            "file://${it.relativePath}"
        }
        runPipelineWith(ModifiedTypeAnnotation(
            generator = javaClass.name,
            commenter = addFileName
        ))
        val assertCode = assertCode()
        assertCode.contains(
            "    comments = \"file://"
        )
    }

    private fun assertGenerated(expectedCode: String) {
        val assertThat = assertCode()
        assertThat.contains(expectedCode)
    }

    private fun assertCode(): StringSubject {
        val code = generatedCode()
        return assertThat(code)
    }

    private fun generatedCode() = sources.first()
        .file(Path(JAVA_FILE))
        .outputPath.readText()

    private fun runPipelineWith(modifiedTypeAnnotation: ModifiedTypeAnnotation) {
        val params = pipelineParams {
            withRoots(sources.first().inputRoot, sources.first().outputRoot)
        }
        Pipeline(
            params = params,
            plugins = listOf(modifiedTypeAnnotation.toPlugin()),
        )()
    }
}

private class FrozenPartyAtTimezone(private val dateTime: ZonedDateTime) :
    FrozenMadHatterParty(dateTime.toInstant().toTimestamp()) {

    override fun currentZone(): ZoneId {
        return dateTime.zone
    }
}
