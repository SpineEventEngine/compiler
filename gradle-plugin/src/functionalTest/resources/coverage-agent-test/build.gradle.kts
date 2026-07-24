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

import io.spine.dependency.test.Jacoco
import io.spine.gradle.repo.standardToSpineSdk
import io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler
import org.gradle.process.CommandLineArgumentProvider

buildscript {
    standardSpineSdkRepositories()
    configurations.all {
        resolutionStrategy {
            force(
                io.spine.dependency.lib.JetBrainsAnnotations.lib,
            )
        }
    }
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

spine {
    compiler {
        plugins(
            "io.spine.tools.compiler.test.UnderscorePrefixRendererPlugin",
            "io.spine.tools.compiler.test.TestPlugin"
        )
    }
}

/**
 * The wiring below is the reference for a consumer build capturing the coverage
 * of the code executed inside the forked Compiler JVM.
 *
 * It resolves the standalone JaCoCo agent and attaches it to each
 * [LaunchSpineCompiler] task via the standard `JavaExec` fork options.
 * The agent writes a per-task `.exec` file under `build/jacoco-compiler/`.
 */
val jacocoAgent: Configuration by configurations.creating

dependencies {
    jacocoAgent("org.jacoco:org.jacoco.agent:${Jacoco.version}:runtime")
}

tasks.withType<LaunchSpineCompiler>().configureEach {
    val execFile = layout.buildDirectory.file("jacoco-compiler/$name.exec")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        val agentJar = jacocoAgent.singleFile.absolutePath
        val destFile = execFile.get().asFile.absolutePath
        listOf("-javaagent:$agentJar=destfile=$destFile,append=true")
    })
    // Record the user classpath, letting the functional test analyze
    // the very jars the forked JVM loaded the Compiler plugins from.
    doLast {
        file("user-classpath.txt").writeText(
            configurations["spineCompiler"].asPath
        )
    }
}
