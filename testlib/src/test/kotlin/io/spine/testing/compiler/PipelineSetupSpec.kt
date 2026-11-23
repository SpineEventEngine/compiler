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

package io.spine.testing.compiler

import com.google.protobuf.Empty
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.format.Format
import io.spine.io.Resource
import io.spine.io.ResourceDirectory
import io.spine.testing.compiler.PipelineSetup.Companion.byResources
import io.spine.testing.compiler.PipelineSetup.Companion.detectCallingClass
import io.spine.tools.code.Java
import io.spine.tools.code.Kotlin
import io.spine.tools.code.TypeScript
import io.spine.tools.compiler.params.PipelineParameters
import io.spine.tools.compiler.plugin.Plugin
import io.spine.tools.compiler.settings.SettingsDirectory
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import io.spine.tools.prototap.Paths.CODE_GENERATOR_REQUEST_JSON_FILE
import io.spine.validation.NonValidated
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`PipelineSetup` should")
internal class PipelineSetupSpec {

    private val classLoader = this::class.java.classLoader

    @Test
    fun `ensure output and settings directories are created`(
        @TempDir workingDir: Path,
        @TempDir input: Path,
        @TempDir output: Path,
    ) {
        // Since JUnit creates `output` and `settings` directories automatically,
        // have nested ones to check the creation.
        val outputPrim = output.resolve("primo")
        val setup = setup(workingDir, input, outputPrim) { _ -> }
        val pipeline = setup.createPipeline()
        val sourceFileSet = pipeline.sources[0]
        sourceFileSet.run {
            inputRoot shouldBe input
            outputRoot.run {
                this shouldBe outputPrim
                exists() shouldBe true
            }
        }
    }

    @Test
    fun `invoke settings callback before creating a pipeline`(
        @TempDir workingDir: Path,
        @TempDir input: Path,
        @TempDir output: Path,
    ) {
        val setup = setup(workingDir, input, output) {
            it.write("foo_bar", Format.ProtoJson, Empty.getDefaultInstance().toByteArray())
        }
        val settingsDir = setup.settings.path
        settingsDir.fileCount() shouldBe 0
        setup.createPipeline()
        settingsDir.fileCount() shouldBe 1
    }

    @Test
    fun `obtain the calling class`() {
        val callingClass = detectCallingClass()
        callingClass shouldBe this::class.java
    }

    @Test
    fun `use 'CodeGeneratorRequest' captured by ProtoTap`(
        @TempDir workingDir: Path,
        @TempDir output: Path
    ) {
        val setup = setupByResources(Java, workingDir, output)
        val pipeline = setup.createPipeline()
        pipeline.request shouldNotBe CodeGeneratorRequest.getDefaultInstance()
    }

    @Test
    fun `create a JSON file with 'CodeGeneratorRequest' captured by ProtoTap`(
        @TempDir workingDir: Path,
        @TempDir output: Path
    ) {
        val language = Java
        setupByResources(language, workingDir, output)
        val resource = Resource.file(
            "${PROTOC_PLUGIN_NAME}/$CODE_GENERATOR_REQUEST_JSON_FILE",
            classLoader
        )
        resource.exists() shouldBe true
    }

    @Test
    fun `use sources captured by ProtoTap`(
        @TempDir workingDir: Path,
        @TempDir output: Path
    ) {
        val language = Java
        val setup = setupByResources(language, workingDir, output)
        val pipeline = setup.createPipeline()
        val sourceFileSet = pipeline.sources[0]
        val langDir = language.protocOutputDir()
        val resourceDir = ResourceDirectory.get("${PROTOC_PLUGIN_NAME}/$langDir", classLoader)
        sourceFileSet.run {
            // We have generated sources as input in the set.
            isEmpty shouldBe false
            val inputPath = resourceDir.toPath()
            inputRoot shouldBe inputPath

            // The output root of the source file set is configured with the subdirectory
            // that corresponds to the input.
            outputRoot shouldBe output.resolve(langDir)
            find(
                Path("io/spine/given/domain/gas/CompressorStation.java")
            ) shouldNotBe null
        }
    }

    @Test
    fun `calculate the name of 'protoc' output directory for a 'Language'`() {
        Java.protocOutputDir() shouldBe "java"
        Kotlin.protocOutputDir() shouldBe "kotlin"
        TypeScript.protocOutputDir() shouldBe "ts"
    }

    @Test
    fun `add compiled proto files to pipeline parameters`(
        @TempDir workingDir: Path,
        @TempDir output: Path,
    ) {
        val setup = setupByResources(Java, workingDir, output)
        val pipeline = setup.createPipeline()

        val files = pipeline.params.compiledProtoList.map { File(it.path) }
        files.forEach {
            it.isFile shouldBe true
            it.isAbsolute shouldBe true
        }

        val fileNames = files.map { it.name }

        // Should be alphabetically sorted.
        fileNames shouldContainExactly listOf("gas_transportation.proto", "oil_refinery.proto")
    }
}

private fun setup(
    workingDir: Path,
    input: Path,
    output: Path,
    writeSettings: (SettingsDirectory) -> Unit
): PipelineSetup = PipelineSetup(
    createParams(workingDir),
    listOf(StubPlugin()),
    input,
    output,
    writeSettings = writeSettings
)

private fun setupByResources(
    language: Java,
    workingDir: Path,
    outputRoot: Path
): PipelineSetup {
    return byResources(
        language,
        createParams(workingDir),
        listOf(StubPlugin()),
        outputRoot,
    ) { _ -> }
}

private fun createParams(workingDir: Path): @NonValidated PipelineParameters =
    parametersForWorkingDir(workingDir)

internal class StubPlugin: Plugin()

private fun Path.fileCount() = toFile().list()?.size ?: 0
