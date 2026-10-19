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

/*
 * This build tests the Compiler artifacts that the root build publishes to Maven Local
 * (see `localPublish` in `../build.gradle.kts`) before it runs this build as its
 * `integrationTest` task.
 *
 * Do not turn this build back into a composite of the root one (`includeBuild("..")`).
 * Gradle would then substitute the `compiler-*` artifacts with the root projects and run
 * their code generation, compilation, and packaging tasks from here, overwriting the
 * outputs in the root project directories. As `integrationTest` runs after the tests of
 * the root build and, usually, before its coverage report, the report would analyze class
 * files that differ from those the tests ran against, and drop the coverage of the
 * mismatched classes.
 *
 * The Compiler artifacts of the version under development come from Maven Local only.
 * So a remote repository that already has this version never serves it instead,
 * and the build fails if the artifacts were not published.
 */
pluginManagement {
    // The dependencies of the plugin resolve via the repositories of the applying build script.
    repositories {
        mavenLocal {
            content {
                includeGroup("io.spine.compiler")
                includeModuleByRegex("io\\.spine\\.tools", "compiler-.*")
            }
        }
        gradlePluginPortal()
    }
    resolutionStrategy.eachPlugin {
        if (requested.id.id == "io.spine.compiler") {
            useVersion(settings.extra["compilerVersion"] as String)
        }
    }
}

apply(from = "$rootDir/../version.gradle.kts")
val compilerVersion = extra["compilerVersion"] as String

include(":consumer")
include(":in-place-consumer")
include(":compiler-extension")

// The projects declare their repositories in their own scripts, via `standardToSpineSdk()`.
// Exclusive content takes precedence over all of them, whatever their order.
gradle.beforeProject {
    listOf(buildscript.repositories, repositories).forEach { repos ->
        repos.exclusiveContent {
            forRepository { repos.mavenLocal() }
            filter {
                includeVersionByRegex("io\\.spine\\.tools", "compiler-.*", Regex.escape(compilerVersion))
            }
        }
    }
}
