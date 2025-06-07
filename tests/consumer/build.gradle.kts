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

@file:Suppress("RemoveRedundantQualifierName")

import io.spine.dependency.lib.JavaX
import io.spine.dependency.local.Base
import io.spine.dependency.local.Spine
import io.spine.tools.compiler.gradle.api.CompilerSettings
import io.spine.tools.gradle.root.rootExtension

buildscript {
    standardSpineSdkRepositories()
    apply(from = "$rootDir/../version.gradle.kts")
    val compilerVersion: String by extra
    dependencies {
        classpath("io.spine.tools:compiler-gradle-plugin:$compilerVersion")
    }
}

apply(plugin = "io.spine.compiler")

dependencies {
    val extensionSubproject = project(":compiler-extension")
    "spineCompiler"(extensionSubproject)
    implementation(extensionSubproject)
    implementation(Base.lib)?.because("`io.spine.annotation.Generated` is used.")
    implementation(JavaX.annotations)?.because("`@javax.annotation.Generated` is used too.")
    testImplementation(Base.lib)?.because("tests use packing and unpacking extension functions.")
}

rootExtension.extensions.getByType<CompilerSettings>().apply {
    plugins(
        "io.spine.tools.compiler.test.uuid.UuidPlugin",
        "io.spine.tools.compiler.test.annotation.AnnotationPlugin"
    )
}
