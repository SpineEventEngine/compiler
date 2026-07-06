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

import io.spine.tools.gradle.testing.GradleProject
import org.gradle.testkit.runner.internal.DefaultGradleRunner

/**
 * Tunes the TestKit runner of this project for the functional tests of
 * the Compiler Gradle plugin.
 *
 * Grants the build JVM generous memory limits — a fixture build compiles
 * Kotlin, runs `protoc`, and launches the Compiler — and disables
 * the Protobuf version check, which is not relevant to the fixture projects.
 */
internal fun GradleProject.tuneRunner() {
    (runner as DefaultGradleRunner).withJvmArguments(
        "-Xmx8g",
        "-XX:MaxMetaspaceSize=1512m",
        "-XX:+UseParallelGC",
        "-XX:+HeapDumpOnOutOfMemoryError"
    ).withEnvironment(
        mapOf("TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK" to "true")
    )
}
