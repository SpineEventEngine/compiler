/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.protodata.gradle.CodegenSettings
import io.spine.internal.dependency.JavaX

@Suppress("RemoveRedundantQualifierName")
buildscript {
    io.spine.internal.gradle.doApplyStandard(repositories)
    apply(from = "$rootDir/../version.gradle.kts")
    val protoDataVersion: String by extra
    dependencies {
        classpath("io.spine.protodata:gradle-plugin:$protoDataVersion")
    }
}

apply(plugin = "io.spine.protodata")

dependencies {
    val extensionSubproject = project(":protodata-extension")
    "protoData"(extensionSubproject)
    implementation(extensionSubproject)
    implementation(JavaX.annotations)
}

extensions.getByType<CodegenSettings>().apply {
    renderers(
        "io.spine.protodata.test.uuid.ClassScopePrinter",
        "io.spine.protodata.test.uuid.UuidJavaRenderer",
        "io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration",

        "io.spine.protodata.test.annotation.PrintFieldGetter",
        "io.spine.protodata.test.annotation.AnnotationRenderer",
        "io.spine.protodata.codegen.java.generado.GenerateGenerated"
    )
    plugins(
        "io.spine.protodata.test.uuid.UuidPlugin",
        "io.spine.protodata.test.annotation.AnnotationPlugin"
    )
}
