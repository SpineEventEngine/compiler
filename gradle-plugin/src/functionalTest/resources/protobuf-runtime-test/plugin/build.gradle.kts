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

import io.spine.gradle.repo.standardToSpineSdk

plugins {
    java
    kotlin("jvm")
    id("com.google.protobuf")
}

repositories {
    mavenLocal()
    standardToSpineSdk()
}

configurations.all {
    resolutionStrategy {
        force(
            io.spine.dependency.local.Base.lib,
        )
    }
}

// The Protobuf release the plugin is built with.
// `protoc` of this release makes the gencode require a runtime not older than it.
val pluginProtobuf = "@PLUGIN_PROTOBUF@"

dependencies {
    implementation("io.spine.tools:compiler-api:@COMPILER_VERSION@")
    // `strictly` keeps the runtime of the plugin on the user classpath
    // even when it is older than the one `compiler-api` asks for.
    implementation("com.google.protobuf:protobuf-java") {
        version {
            strictly(pluginProtobuf)
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$pluginProtobuf"
    }
}
