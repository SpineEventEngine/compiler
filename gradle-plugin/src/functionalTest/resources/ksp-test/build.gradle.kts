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

import com.google.protobuf.gradle.protobuf
import io.spine.dependency.lib.Protobuf
import io.spine.gradle.repo.standardToSpineSdk

buildscript {
    standardSpineSdkRepositories()
}

group = "io.spine.tools.test"
version = "1.0.0-SNAPSHOT"

plugins {
    kotlin("jvm")
    id("com.google.protobuf")
    id("@COMPILER_PLUGIN_ID@") version "@COMPILER_VERSION@"
    // Apply KSP after the Compiler plugin so that the KSP tasks are registered later than
    // the `LaunchSpineCompiler` tasks. This reproduces the task-registration order under which
    // the Compiler plugin must still arrange the dependency of the KSP task on the launch task.
    // The version must match `io.spine.dependency.build.Ksp.version`.
    id("com.google.devtools.ksp") version "2.3.6"
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

/**
 * Prints the names of the tasks on which the `kspKotlin` task depends.
 *
 * Used by the functional test to verify that the Compiler plugin arranged
 * the dependency of the KSP task on the `launchSpineCompiler` task.
 */
tasks.register("printKspDependencies") {
    doLast {
        val kspTask = tasks.getByName("kspKotlin")
        val dependencies = kspTask.taskDependencies.getDependencies(kspTask)
            .joinToString(separator = ",") { it.name }
        println("KSP_DEPENDENCIES=[$dependencies]")
    }
}
