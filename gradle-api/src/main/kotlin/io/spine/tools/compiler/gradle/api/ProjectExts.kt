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

package io.spine.tools.compiler.gradle.api

import io.spine.tools.compiler.params.Directories.COMPILER_WORKING_DIR
import io.spine.tools.gradle.lib.spineExtension
import io.spine.tools.gradle.root.rootWorkingDir
import io.spine.tools.meta.MavenArtifact
import java.io.File
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.Directory
import org.gradle.api.tasks.SourceSet

/**
 * Obtains the directory where the Compiler stores its temporary files.
 */
public val Project.compilerWorkingDir: Directory
    get() = rootWorkingDir.dir(COMPILER_WORKING_DIR)

/**
 * Obtains the instance of [CompilerSettings] extension of this project.
 */
public val Project.compilerSettings: CompilerSettings
    get() = spineExtension<CompilerSettings>()

/**
 * Obtains the path of the directory with the generated code as configured by
 * the [CompilerSettings.outputBaseDir] property of the the Compiler extension
 * of this Gradle project.
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

/**
 * Adds the given artifacts to the [spineCompiler][Names.USER_CLASSPATH_CONFIGURATION]
 * configuration of this project.
 */
public fun Project.addUserClasspathDependency(vararg artifacts: MavenArtifact): Unit =
    artifacts.forEach {
        addDependency(Names.USER_CLASSPATH_CONFIGURATION, it)
    }

private fun Project.addDependency(configuration: String, artifact: MavenArtifact) {
    val dependency = findDependency(artifact) ?: artifact.coordinates
    dependencies.add(configuration, dependency)
}

private fun Project.findDependency(artifact: MavenArtifact): Dependency? {
    val dependencies = configurations.flatMap { c -> c.dependencies }
    val found = dependencies.firstOrNull { d ->
        artifact.group == d.group // `d.group` could be `null`.
                && artifact.name == d.name
    }
    return found
}
