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

package io.spine.tools.compiler.backend.perf

import com.google.protobuf.compiler.codeGeneratorRequest
import io.kotest.matchers.comparables.shouldBeLessThan
import io.spine.string.simply
import io.spine.testing.compiler.RenderingTestbed
import io.spine.testing.compiler.pipelineParams
import io.spine.testing.compiler.withRequestFile
import io.spine.testing.compiler.withRoots
import io.spine.tools.compiler.ast.toAbsoluteFile
import io.spine.tools.compiler.backend.Pipeline
import io.spine.tools.compiler.backend.createTypeSystem
import io.spine.tools.compiler.test.DoctorProto
import io.spine.tools.compiler.test.Journey
import io.spine.tools.compiler.test.TestPlugin
import io.spine.tools.compiler.test.UnderscorePrefixRenderer
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * A performance smoke signal for the compiler engine.
 *
 * The engine builds a full event-sourced `Code Generation` context
 * ([CodegenContext][io.spine.tools.compiler.context.CodegenContext]) on every
 * [Pipeline] run, so a regression in that bootstrap or in the rendering path
 * inflates the compilation time of every downstream build. This test times a
 * single cold pipeline run over the `test-env` fixtures, logs the elapsed time as a
 * signal, and fails only if the run exceeds a deliberately generous [HANG_CEILING].
 *
 * This is **not** a performance gate: the ceiling is a deadlock/pathology guard,
 * not a budget (see audit `docs/audit-2026-06.md`, open question 4). The test is
 * tagged [`performance`][Tag] so it is excluded from the regular `test` task and
 * runs only via the `:backend:performanceTest` task wired into the
 * `Engine performance smoke test` workflow.
 */
@Tag("performance")
@DisplayName("`Pipeline` performance smoke test should")
internal class PipelineSmokeSpec {

    @Test
    fun `finish a pipeline run within the hang ceiling`(@TempDir sandbox: Path) {
        val srcRoot = sandbox.resolve("src").createDirectories()
        val targetRoot = sandbox.resolve("target").createDirectories()
        val codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        // The correctness of the rendered code is irrelevant here; we only need the
        // render path to execute over a real source file.
        srcRoot.resolve("SourceCode.java").writeText("${simply<Journey>()} worth taking")

        val descriptor = DoctorProto.getDescriptor()
        val request = codeGeneratorRequest {
            protoFile += descriptor.toProto()
            fileToGenerate += descriptor.name
        }
        codegenRequestFile.writeBytes(request.toByteArray())

        val typeSystem = createTypeSystem(request)
        val params = pipelineParams {
            addAllCompiledProto(typeSystem.compiledProtoFiles.files.map { it.toAbsoluteFile() })
            withRequestFile(codegenRequestFile)
            withRoots(srcRoot, targetRoot)
        }
        val renderer = UnderscorePrefixRenderer()

        val elapsed = measureTime {
            Pipeline(
                params = params,
                additionalPlugins = listOf(TestPlugin(), RenderingTestbed(renderer)),
            )()
        }

        // The signal: surfaced in the build log via `testLogging.showStandardStreams`.
        println("[perf] engine pipeline run over `doctor.proto`: $elapsed")

        // The deadlock guard — NOT a performance budget.
        elapsed shouldBeLessThan HANG_CEILING
    }

    private companion object {

        /**
         * Deliberately generous. A single small pipeline run takes a few seconds at
         * most on a CI runner; this trips only on a deadlock or pathological slowdown,
         * never on normal performance variance. It is intentionally **not** a
         * performance budget (audit open question 4).
         */
        private val HANG_CEILING = 60.seconds
    }
}
