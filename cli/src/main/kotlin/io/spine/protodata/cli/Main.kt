/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.protodata.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.io.Resource
import io.spine.protodata.Pipeline
import io.spine.protodata.option.OptionsProvider
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceSet
import java.io.File
import java.io.File.pathSeparator
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * The resource file containing the version of ProtoData.
 *
 * Such a resource name might be duplicated in other places in ProtoData code base. The reason for
 * this is to avoid creating an extra dependencies. Search by the string value of this constant
 * when making changes.
 */
private const val VERSION_FILE_NAME = "version.txt"

/**
 * Launches the CLI application.
 *
 * When the application is done or an unhandled error occurs, exits the process.
 */
public fun main(args: Array<String>): Unit =
    Run(readVersion()).main(args)

private fun readVersion(): String {
    val versionFile = Resource.file(VERSION_FILE_NAME, Run::class.java.classLoader)
    return versionFile.read()
}

/**
 * The main CLI command which performs the ProtoData code generation tasks.
 *
 * The command accepts class names for the service provider interface implementations via the CLI
 * parameters, such as `--plugin`, `--renderer`, and `--options`, all of which can be repeated
 * parameters, if required. Then, using the classpath of the app and the user classpath supplied
 * via the `--user-classpath` parameter, loads those classes. `Code Generation` context accept
 * Protobuf compiler events, regarding the Protobuf types, listed in
 * the `CodeGeneratorRequest.file_to_generate` as loaded from the `--request` parameter. Finally,
 * the renderers apply required changes to the source set with the root path, supplied in
 * the `--source-root` parameter.
 */
internal class Run(version: String) : CliktCommand(
    name = "protodata",
    help = "ProtoData tool helps build better multi-platform code generation. Version ${version}.",
    epilog = "https://github.com/SpineEventEngine/ProtoData/",
    printHelpOnEmptyArgs = true
) {

    private val plugins: List<String> by option("--plugin", "-p", help = """
        The name of a Java class, a subtype of `${Plugin::class.qualifiedName}`.
        There can be multiple providers. To pass more then one value, type:
           `<...> -p com.foo.MyEntitiesPlugin -p com.foo.OtherEntitiesPlugin`
    """.trimIndent()).multiple()
    private  val renderers: List<String> by option("--renderer", "-r", help = """
        The name of a Java class, a subtype of `${Renderer::class.qualifiedName}`.
        There can only be multiple renderers. To pass more then one value, type:
           `<...> -r com.foo.MyJavaRenderer -r com.foo.MyKotlinRenderer`
    """.trimIndent()).multiple(required = true)
    private val optionProviders: List<String> by option("--options", "-o", help = """
        The name of a Java class, a subtype of `${OptionsProvider::class.qualifiedName}`.
        There can be multiple providers. To pass more then one value, type:
           `<...> -o com.foo.TypeOptionsProvider -o com.foo.FieldOptionsProvider`
    """.trimIndent()).multiple()
    private val codegenRequestFile: File by option("--request", "-t", help =
    "The path to the binary file containing a serialized instance of " +
            "`${CodeGeneratorRequest.getDescriptor().name}`."
    ).file(
        mustExist = true,
        canBeDir = false,
        canBeSymlink = false,
        mustBeReadable = true
    ).required()
    private val sourceRoot: Path by option("--source-root", "--src", help = """
        The path to a directory which contains the source files to be processed.
    """.trimIndent()
    ).path(
        mustExist = true,
        canBeFile = false,
        canBeSymlink = false
    ).required()
    private val classPath: List<Path>? by option("--user-classpath" ,"--ucp", help = """
        The user classpath which contains all `--renderer` classes, user-defined policies, views,
        events, etc., as well as all their dependencies, which are not included as a part of
        the ProtoData library. This may be omitted if the classes are already present in
        the ProtoData classpath. May be one path to a JAR, a ZIP, or a directory. Or may be many
        paths separated by the `$pathSeparator` separator char (system-dependent).
    """.trimIndent()
    ).path(
        mustExist = true,
        mustBeReadable = true
    ).split(pathSeparator)

    override fun run() {
        val plugins = loadPlugins()
        val optionsProviders = loadOptions()
        val renderer = loadRenderers()
        val sourceSet = SourceSet.from(sourceRoot, sourceRoot)

        val registry = ExtensionRegistry.newInstance()
        optionsProviders.forEach { it.dumpTo(registry) }
        val codegenRequest = codegenRequestFile.inputStream().use {
            CodeGeneratorRequest.parseFrom(it, registry)
        }
        Pipeline(plugins, renderer, sourceSet, codegenRequest)()
    }

    private fun loadPlugins() =
        load(PluginBuilder(), plugins)

    private fun loadRenderers() =
        load(RendererBuilder(), renderers)

    private fun loadOptions() =
        load(OptionsProviderBuilder(), optionProviders)

    private fun <T: Any> load(builder: ReflectiveBuilder<T>, classNames: List<String>): List<T> {
        val classLoader = Thread.currentThread().contextClassLoader
        return classNames.map { builder.tryCreate(it, classLoader) }
    }

    private fun <T: Any> ReflectiveBuilder<T>.tryCreate(className: String,
                                                        classLoader: ClassLoader): T {
        try {
            return createByName(className, classLoader)
        } catch (e: ClassNotFoundException) {
            error(e.message)
            error("Please add the required class `$className` to the user classpath.")
            if (classPath != null) {
                error("User classpath contains: `${classPath!!.joinToString(pathSeparator)}`.")
            }
            exitProcess(1)
        }
    }

    private fun error(msg: String?) {
        echo(msg, err = true)
    }
}
