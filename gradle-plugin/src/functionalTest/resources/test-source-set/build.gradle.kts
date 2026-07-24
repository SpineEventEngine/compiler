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

import com.google.protobuf.gradle.ProtobufExtension
import io.spine.dependency.lib.Protobuf
import io.spine.gradle.repo.standardToSpineSdk
import org.gradle.api.tasks.SourceSetContainer

buildscript {
    standardSpineSdkRepositories()
}

group = "io.spine.tools.test"
version = "1.0.0-SNAPSHOT"

plugins {
    java
    kotlin("jvm")
    id("com.google.protobuf")
    id("@COMPILER_PLUGIN_ID@") version "@COMPILER_VERSION@"
}

repositories {
    mavenLocal() // Must come first for `compiler-test-env`.
    standardToSpineSdk()
}

spine {
    compiler {
        plugins(
            "io.spine.tools.compiler.test.NoOpRendererPlugin",
            "io.spine.tools.compiler.test.TestPlugin"
        )
    }
}
configurations.all {
    resolutionStrategy {
        force(
            io.spine.dependency.local.Base.lib,
        )
    }
}

dependencies {
    spineCompiler("io.spine.tools:compiler-test-env:+")
    Protobuf.libs.forEach { implementation(it) }
}

// Simulate the `protoc` output directory leaking back into the `test` source set.
//
// The Compiler reads the `protoc` output from `build/generated/sources/proto/test`
// and writes its own output under `generated/test`. The plugin removes the
// `protoc` directories from the source sets to avoid compiling the same class
// twice. That removal is a one-time rewrite of the source-set directories and is
// fragile: depending on the plugin-configuration order, a symlinked project path,
// or a consumer plugin re-adding the directory, the `protoc` output may end up in
// the `test` source set anyway. We reproduce that final state directly by
// re-adding the directory after the plugin has configured the source set.
//
// Without compile-task-level filtering, `compileTestJava` and `compileTestKotlin`
// then see each generated class twice and fail with "duplicate class" errors.
afterEvaluate {
    val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
    val test = sourceSets.getByName("test")
    // The `protoc` output base directory of the Protobuf Gradle Plugin
    // (`build/generated/sources/proto` by default).
    val protocBaseDir = project.extensions.getByType(ProtobufExtension::class.java)
        .generatedFilesBaseDir
    test.java.srcDir("$protocBaseDir/test/java")
    test.java.srcDir("$protocBaseDir/test/kotlin")
}
