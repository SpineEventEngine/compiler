/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import io.spine.dependency.test.JUnit
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.Jackson.DataFormat
import io.spine.dependency.local.CoreJava
import io.spine.dependency.local.ToolBase
import io.spine.gradle.publish.CheckVersionIncrement
import io.spine.gradle.publish.IncrementGuard
import io.spine.gradle.publish.PublishingRepos

plugins {
    module
    `java-test-fixtures`
    `build-proto-model`
    prototap
}

dependencies {
    api(CoreJava.server)
    api(ToolBase.lib)
    api(project(":api"))
    implementation(project(":params"))?.because("We need the `PipelineParameters` type.")

    api(platform(Jackson.bom))
    with(Jackson) {
        api(databind)
        implementation(DataFormat.yaml)
        runtimeOnly(moduleKotlin)
    }

    testImplementation(project(":testlib"))
    testImplementation(project(":test-env"))
    testImplementation(JUnit.Jupiter.params)
}

apply<IncrementGuard>()

tasks.withType<CheckVersionIncrement> {
    repository = PublishingRepos.cloudArtifactRegistry
}

val nl: String = System.lineSeparator()

/**
 * Prints output directories produced by `launchProtoData` tasks with
 * corresponding number of files in those directories.

val launchProtoData: Task by tasks.getting {
    doLast {
        println("***** `launchProtoData.output`:")
        outputs.files.forEach { dir ->
            val fileCount = project.fileTree(dir).count()
            println("$dir $fileCount files.")
        }
        println("*************************")
    }
}
 */

/**
 * Prints input of `compileKotlin` tasks of this module.

val compileKotlin: Task by tasks.getting {
    doFirst {
        println()
        println(">>>> Kotlin source set dirs:")
        sourceSets.main { kotlin.srcDirs.forEach { dir -> println("$dir")} }
        println(">>>>> `compileKotlin` task inputs in `doFirst` :")
        println(inputs.files.joinToString(separator = nl))
    }
}
 */
