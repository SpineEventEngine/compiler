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

package io.spine.compiler.gradle.plugin

import com.google.common.annotations.VisibleForTesting
import io.spine.compiler.gradle.api.CompilerSettings
import io.spine.compiler.gradle.api.Names.EXTENSION_NAME
import io.spine.tools.fs.DirectoryName.generated
import io.spine.tools.gradle.DslSpec
import io.spine.tools.gradle.protobuf.generatedSourceProtoDir
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.listProperty

/**
 * The `compiler { }` Gradle project extension.
 */
public class Extension(private val project: Project): CompilerSettings {

    private val factory = project.objects

    /**
     * Adds the class names of the plugins to the list of those passed to ProtoData.
     */
    public override fun plugins(vararg classNames: String): Unit =
        plugins.addAll(classNames.toList())

    /**
     * Contains the class names of the plugins passed to ProtoData.
     */
    @VisibleForTesting
    public val plugins: ListProperty<String> =
        factory.listProperty<String>().convention(listOf())

    /**
     * Synthetic property for providing the source directories for the given
     * source set under [Project.generatedSourceProtoDir].
     *
     * @see sourceDirs
     */
    private val srcBaseDirProperty: DirectoryProperty = with(project) {
        objects.directoryProperty().convention(provider {
            layout.projectDirectory.dir(generatedSourceProtoDir.toString())
        })
    }

    /**
     * Allows configuring the subdirectories under the generated source set.
     *
     * Defaults to [defaultSubdirectories].
     */
    public override var subDirs: List<String>
        get() = subDirProperty.get()
        set(value) {
            if (value.isNotEmpty()) {
                subDirProperty.set(value)
            }
        }

    private val subDirProperty: ListProperty<String> =
        factory.listProperty<String>().convention(defaultSubdirectories)

    @Deprecated(
        message = "Please use `outputBaseDirectory` instead.",
        ReplaceWith("outputBaseDirectory")
    )
    public override var targetBaseDir: Any
        get() = outputBaseDir.get()
        set(value) = outputBaseDir.set(project.file(value))

    /**
     * Allows configuring the path to the root directory with the generated code.
     *
     * By default, points at the `$projectDir/generated/` directory.
     */
    override val outputBaseDir: DirectoryProperty = with(project) {
        objects.directoryProperty().convention(
            layout.projectDirectory.dir(generated.name)
        )
    }

    /**
     * Obtains the source directories for the given source set.
     */
    internal fun sourceDirs(sourceSet: SourceSet): Provider<List<Directory>> =
        compileDir(sourceSet, srcBaseDirProperty)

    /**
     * Obtains the target directories for code generation.
     *
     * @see outputBaseDir for the rules for the target dir construction
     */
    internal fun outputDirs(sourceSet: SourceSet): Provider<List<Directory>> =
        compileDir(sourceSet, outputBaseDir)

    private fun compileDir(
        sourceSet: SourceSet,
        base: DirectoryProperty
    ): Provider<List<Directory>> {
        val sourceSetDir = base.dir(sourceSet.name)
        return sourceSetDir.map { root: Directory ->
            subDirs.map { root.dir(it) }
        }
    }

    public companion object {

        /**
         * Default subdirectories expected by ProtoData under a generated source set.
         *
         * @see subDirs
         */
        public val defaultSubdirectories: List<String> = listOf(
            "java",
            "kotlin",
            "grpc",
            "js",
            "dart",
        )
    }
}

/**
 * Overrides [DslSpec] for customizing creation of the [Extension].
 */
internal class CompilerDslSpec :
    DslSpec<CompilerSettings>(EXTENSION_NAME, CompilerSettings::class) {

    /**
     * The function for obtaining a project to which the [Plugin] is applied.
     *
     * This property is injected at the [Plugin] constructor.
     */
    internal lateinit var project: () -> Project

    override fun createIn(container: ExtensionContainer): CompilerSettings {
        val extension = Extension(project())
        container.add(extensionClass.java, name, extension)
        return extension
    }
}
