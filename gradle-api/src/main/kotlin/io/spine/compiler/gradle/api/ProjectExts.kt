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

package io.spine.compiler.gradle.api

import org.gradle.api.Project
import io.spine.compiler.params.Directories.PROTODATA_WORKING_DIR
import java.io.File
import java.nio.file.Path
import org.gradle.api.file.Directory
import org.gradle.api.tasks.SourceSet

//TODO:2025-05-29:alexander.yevsyukov: Nest it under the working directory of the `RootPlugin`.
/**
 * Obtains the directory where ProtoData stores its temporary files.
 */
public val Project.compilerWorkingDir: Directory
    get() = layout.buildDirectory.dir(PROTODATA_WORKING_DIR).get()

//TODO:2025-05-29:alexander.yevsyukov: Nest it under the root extension.
/**
 * Obtains the instance of [CompilerSettings] extension of this project.
 */
public val Project.compilerSettings: CompilerSettings
    get() = extensions.findByType(CompilerSettings::class.java)!!

/**
 * Obtains the path of the directory with the generated code as configured by
 * the [CompilerSettings.outputBaseDir] property of the ProtoData extension of this Gradle project.
 */
public val Project.generatedDir: Path
    get() = compilerSettings.outputBaseDir.get().asFile.toPath()

/**
 * Obtains the `generated` directory for the given [sourceSet] and a language.
 *
 * If the language is not given, the returned directory is the root directory for the source set.
 */
public fun Project.generatedDir(sourceSet: SourceSet, language: String = ""): File {
    val path = generatedDir.resolve("${sourceSet.name}/$language")
    return path.toFile()
}
