/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.compiler.gradle.plugin

import io.spine.annotation.VisibleForTesting
import io.spine.tools.compiler.gradle.api.CompilerSettings
import io.spine.tools.compiler.gradle.api.Names.EXTENSION_NAME
import io.spine.tools.compiler.gradle.plugin.Extension.Companion.defaultSubdirectories
import io.spine.tools.fs.DirectoryName.generated
import io.spine.tools.gradle.DslSpec
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

/**
 * The `compiler { }` Gradle project extension.
 */
public class Extension(private val project: Project): CompilerSettings {

    private val factory = project.objects

    /**
     * Adds the class names of the plugins to the list of those passed to the Compiler.
     */
    public override fun plugins(vararg classNames: String): Unit =
        plugins.addAll(classNames.toList())

    /**
     * Contains the class names of the plugins passed to the Compiler.
     */
    @VisibleForTesting
    public val plugins: ListProperty<String> =
        factory.listProperty<String>().convention(listOf())

    /**
     * Synthetic property for providing the source directories for the given
     * source set under [Project.protocOutputDir].
     *
     * @see sourceDirs
     */
    private val srcBaseDirProperty: DirectoryProperty = with(project) {
        objects.directoryProperty().convention(provider {
            layout.projectDirectory.dir(protocOutputDir.toString())
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
     * Allows disabling the Protobuf version check in the Compiler process.
     *
     * Not set by default, so the Compiler process inherits the setting from
     * the environment of the build.
     *
     * @see CompilerSettings.disableProtobufVersionCheck
     */
    public override val disableProtobufVersionCheck: Property<Boolean> =
        factory.property<Boolean>()

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
         * Default subdirectories expected by the Compiler under a generated source set.
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
