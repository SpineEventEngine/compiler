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

package io.spine.tools.compiler.cli.app

import com.google.protobuf.compiler.codeGeneratorRequest
import com.google.protobuf.stringValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.spine.base.Time
import io.spine.format.Format
import io.spine.format.parse
import io.spine.protobuf.pack
import io.spine.string.ti
import io.spine.testing.compiler.googleProtobufProtos
import io.spine.testing.compiler.spineOptionProtos
import io.spine.time.LocalDates
import io.spine.time.Month.SEPTEMBER
import io.spine.time.toInstant
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.ast.AstProto
import io.spine.tools.compiler.ast.FileProto
import io.spine.tools.compiler.ast.toAbsoluteFile
import io.spine.tools.compiler.ast.toDirectory
import io.spine.tools.compiler.ast.toProto
import io.spine.tools.compiler.cli.given.DefaultOptionsCounterPlugin
import io.spine.tools.compiler.cli.given.DefaultOptionsCounterRenderer
import io.spine.tools.compiler.cli.given.DefaultOptionsCounterRendererPlugin
import io.spine.tools.compiler.cli.test.TestOptionsProto
import io.spine.tools.compiler.cli.test.TestProto
import io.spine.tools.compiler.params.PipelineParameters
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.compiler.params.pipelineParameters
import io.spine.tools.compiler.plugin.Plugin
import io.spine.tools.compiler.test.ECHO_FILE
import io.spine.tools.compiler.test.EchoRenderer
import io.spine.tools.compiler.test.EchoRendererPlugin
import io.spine.tools.compiler.test.Project
import io.spine.tools.compiler.test.ProjectProto
import io.spine.tools.compiler.test.ProtoEchoRenderer
import io.spine.tools.compiler.test.ProtoEchoRendererPlugin
import io.spine.tools.compiler.test.TestPlugin
import io.spine.tools.compiler.test.UnderscorePrefixRendererPlugin
import io.spine.tools.compiler.test.echo
import io.spine.tools.prototap.CompiledProtosFile
import io.spine.type.toCompactJson
import io.spine.type.toJson
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("The Compiler command-line application should")
class MainSpec {

    private lateinit var workingDir: WorkingDirectory
    private lateinit var parametersFile: File
    private lateinit var srcRoot : Path
    private lateinit var targetRoot : Path
    private lateinit var codegenRequestFile: Path
    private lateinit var targetFile: Path

    private val outputEchoFile: Path
        get() = targetRoot.resolve(ECHO_FILE)

    @BeforeEach
    fun prepareSources(@TempDir sandbox: Path) {
        workingDir = WorkingDirectory(sandbox)

        codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        srcRoot = sandbox.resolve("src")
        srcRoot.toFile().mkdirs()
        targetRoot = sandbox.resolve("target")
        targetRoot.toFile().mkdirs()

        val compiledProtos = CompiledProtosFile(this::class.java.classLoader)
        val thisModuleFiles = compiledProtos.listFiles { File(it).toProto() }

        val testEnvJar = ProjectProto::class.java.protectionDomain.codeSource.location
        val urlClassLoader = URLClassLoader(arrayOf(testEnvJar), null)
        val testEnvJarFiles = CompiledProtosFile(urlClassLoader).listFiles { File(it).toProto() }

        val params = pipelineParameters {
            compiledProto.addAll(thisModuleFiles + testEnvJarFiles)
            settings = workingDir.settingsDirectory.path.toDirectory()
            sourceRoot.add(srcRoot.toDirectory())
            targetRoot.add(this@MainSpec.targetRoot.toDirectory())
            request = codegenRequestFile.toFile().toAbsoluteFile()
        }
        parametersFile = workingDir.parametersDirectory.write(SourceSetName.test, params)

        val sourceFile = srcRoot.resolve("SourceCode.java")
        sourceFile.writeText("""
            ${Project::class.simpleName}.getUuid() 
        """.trimIndent())
        targetFile = targetRoot.resolve(sourceFile.name)

        val project = ProjectProto.getDescriptor()
        val testProto = TestProto.getDescriptor()
        val request = codeGeneratorRequest {
            protoFile.addAll(
                listOf(
                    project.toProto(),
                    testProto.toProto(),
                    TestOptionsProto.getDescriptor().toProto(),
                    AstProto.getDescriptor().toProto(),
                    FileProto.getDescriptor().toProto(),
                ) + spineOptionProtos()
                        + googleProtobufProtos()
            )
            fileToGenerate.addAll(listOf(
                project.name,
                testProto.name
            ))
        }
        codegenRequestFile.writeBytes(request.toByteArray())
    }

    @Test
    fun `render enhanced code`() {
        launchApp(
            TestPlugin::class,
            UnderscorePrefixRendererPlugin::class
        )
        targetFile.readText() shouldBe "_${Project::class.simpleName}.getUuid() "
    }

    @Test
    fun `provide Spine options by default`() {
        launchApp(
            DefaultOptionsCounterPlugin::class,
            DefaultOptionsCounterRendererPlugin::class
        )
        val generatedFile = targetRoot.resolve(DefaultOptionsCounterRenderer.FILE_NAME)
        generatedFile.readText() shouldBe "true, true"
    }

    @Test
    fun `load settings via a file`() {
        val name = "Internet"
        workingDir.settingsDirectory.writeFor<EchoRenderer>(
            Format.Json, """
                { "value": "$name" }
            """.ti()
        )
        launchApp(EchoRendererPlugin::class)
        outputEchoFile.readText() shouldBe name
    }

    @Nested
    inner class `Receive custom configuration as` {

        @Test
        fun `plain JSON`() {
            val name = "Internet"
            workingDir.settingsDirectory.writeFor<EchoRenderer>(
                Format.Json, """
                    { "value": "$name" }
                """.ti()
            )
            launchApp(EchoRendererPlugin::class)
            outputEchoFile.readText() shouldBe name
        }

        @Test
        fun `Protobuf JSON`() {
            val time = Time.currentTime()
            val json = echo {
                message = "English, %s!"
                extraMessage = stringValue { value = "Do you speak it?" }
                arg = stringValue { value = "Adam Falkner" }.pack()
                when_ = time
            }.toCompactJson()
            workingDir.settingsDirectory.writeFor<ProtoEchoRenderer>(Format.ProtoJson, json)

            launchApp(ProtoEchoRendererPlugin::class)
            val text = outputEchoFile.readText()

            text shouldStartWith time.toInstant().toString()
            text shouldEndWith "English, Adam Falkner!:Do you speak it?"
        }

        @Test
        fun `binary Protobuf`() {
            val time = LocalDates.of(1962, SEPTEMBER, 12)
            val bytes = echo {
                message = "We choose to go to the %s."
                extraMessage = stringValue { value = "and do the other things" }
                arg = stringValue { value = "Moon" }.pack()
                when_ = time.toTimestamp()
            }.toByteArray()

            workingDir.settingsDirectory.writeFor<ProtoEchoRenderer>(Format.ProtoBinary, bytes)

            launchApp(ProtoEchoRendererPlugin::class)

            val text = outputEchoFile.readText()

            text shouldStartWith time.toInstant().toString()
            text shouldEndWith "We choose to go to the Moon.:and do the other things"
        }

        @Suppress("TestFunctionName")
        @Test
        fun YAML() {
            val name = "Mr. Anderson"
            workingDir.settingsDirectory.writeFor<EchoRenderer>(
                Format.Yaml, """
                    value: $name
                """.trimIndent()
            )

            launchApp(EchoRendererPlugin::class)
            outputEchoFile.readText() shouldBe name
        }
    }
    
    private fun launchApp(vararg plugins: KClass<out Plugin>) {
        addPluginClassNames(plugins)
        Run("42.0.0").parse(listOf(
            "--params", parametersFile.absolutePath
        ))
    }

    private fun addPluginClassNames(plugins: Array<out KClass<out Plugin>>) {
        val draftParams = parse<PipelineParameters>(parametersFile)
        val classNames = plugins.map { it.jvmName }
        val withPlugins = draftParams.toBuilder()
            .addAllPluginClassName(classNames)
            .build()
        parametersFile.writeText(withPlugins.toJson())
    }
}
