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

import com.google.protobuf.gradle.protobuf
import io.spine.gradle.repo.standardToSpineSdk

buildscript {
    standardSpineSdkRepositories()
}

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

configurations.all {
    resolutionStrategy {
        force(
            io.spine.dependency.local.Base.lib,
        )
    }
}

dependencies {
    spineCompiler("io.spine.tools:compiler-test-env:+")
}

protobuf {
    protoc {
        artifact = io.spine.dependency.lib.Protobuf.compiler
    }
}

spine {
    compiler {
        plugins(
            "io.spine.tools.compiler.test.UnderscorePrefixRendererPlugin",
            "io.spine.tools.compiler.test.TestPlugin"
        )
    }
}
