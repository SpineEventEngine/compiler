/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
// `protoc` of this release generates code that requires a runtime at least as new
// as the release.
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
