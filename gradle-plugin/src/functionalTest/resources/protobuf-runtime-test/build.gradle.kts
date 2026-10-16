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
import io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler

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
    mavenLocal()
    standardToSpineSdk()
}

// Unlike Spine builds, this project does not force `Protobuf.libs`, so the Protobuf
// runtime of the Compiler JVM comes from dependency resolution alone.
// Only `Base.lib` is forced, as in the other fixtures.
configurations.all {
    resolutionStrategy {
        force(
            io.spine.dependency.local.Base.lib,
        )
    }
}

dependencies {
    spineCompiler(project(":plugin"))
}

spine {
    compiler {
        plugins("io.spine.tools.compiler.test.GencodePlugin")
    }
}

tasks.withType<LaunchSpineCompiler>().configureEach {
    // The functional tests turn off the Protobuf gencode/runtime version check
    // for the whole build. Turn it back on in the Compiler JVM, where the gencode
    // of the plugin must not find a runtime older than itself.
    environment.remove("TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK")
    // Record the classpath of the Compiler JVM, letting the functional test
    // check which Protobuf runtime comes first.
    val classpathFile = layout.projectDirectory.file("compiler-classpath.txt")
    doLast {
        classpathFile.asFile.writeText(classpath.asPath)
    }
}
