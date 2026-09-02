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

import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation

buildscript {
    standardSpineSdkRepositories()
    configurations.all {
        resolutionStrategy {
            // The CoreJvm Compiler plugin on the classpath below still carries
            // the `org.jetbrains:annotations:23.0.0` requirement, which clashes
            // with the `strictly 13.0` pin Gradle puts on build script
            // classpaths ("Pinned to the embedded Kotlin").
            // TODO:2026-07-24:alexander.yevsyukov: Remove this force once the
            //  CoreJvm Compiler excludes the module from its published
            //  dependencies the way the Spine Compiler Gradle plugin does.
            //
            // The `Logging.grpcContext` force formerly kept here became
            // unnecessary with the CoreJvm `.521` bump: the integration tests
            // pass without it.
            force(
                io.spine.dependency.lib.JetBrainsAnnotations.lib,
                // The refresh-era plugin jars carry Protobuf 4.36 gencode; the
                // runtime must not be older, so pin it to the baseline.
                io.spine.dependency.lib.Protobuf.javaLib,
            )
        }
    }
    apply(from = "$rootDir/../version.gradle.kts")

    val compilerVersion = extra["compilerVersion"] as String
    dependencies {
        classpath(spineCompiler.pluginLib(compilerVersion))
        classpath(coreJvmCompiler.gradlePlugin)
    }
}

apply {
    plugin("io.spine.core-jvm")
}

configurations.all {
    resolutionStrategy {
        // The legacy `spine-tool-base` coordinates used to be substituted with the
        // monolithic `tool-base` module. That target is no longer published, so the
        // rule cannot be kept, and nothing on the classpath requests the legacy
        // coordinates any more.
        force(
            ToolBase.code,
            ToolBase.fs,
            ToolBase.javaCode,
            ToolBase.kotlinCode,
            ToolBase.protoCode,
            ToolBase.intellijPlatform,
            ToolBase.intellijPlatformJava,
            Validation.runtime,
            Validation.javaBundle
        )
    }
}

val compilerVersion = extra["compilerVersion"] as String

dependencies {
    compileOnly("io.spine.tools:compiler-backend:$compilerVersion")
    compileOnly("io.spine.tools:compiler-jvm:$compilerVersion")
}
