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

package io.spine.tools.compiler.gradle.api

import io.spine.tools.meta.Module

/**
 * Constants for locating the Compiler artifacts in Maven repositories.
 */
@Suppress("ConstPropertyName") // https://bit.ly/kotlin-prop-names
public object Artifacts {

    /**
     * The Maven group of the Compiler artifacts.
     */
    public const val group: String = "io.spine.tools"

    /**
     * The name of the Compiler Backend artifact.
     */
    public val compilerBackend: Module = Module(group, "compiler-backend")

    /**
     * The module of the Compiler Gradle Plugin artifact.
     */
    public val compilerGradlePlugin: Module = Module(group, "compiler-gradle-plugin")

    /**
     * The Maven group of the Google Protobuf artifacts, such as `protoc` and
     * the runtime libraries like `protobuf-java`.
     */
    public const val protobufGroup: String = "com.google.protobuf"

    /**
     * The module of the `protoc` artifact of Google Protobuf.
     */
    public val protobufProtocArtifact: Module = Module(protobufGroup, "protoc")

    /**
     * Obtains Maven coordinates of the `fat-cli` variant of the command-line application.
     *
     * "fat-cli" is an all-in-one distribution of the Compiler, published somewhat in the past.
     * Ironically, we need it in the Compiler development.
     * It removes the dependency conflicts between Compiler-s.
     *
     * Please make sure that the module name in this constant (`cli-all`) has the same value
     * as the `MavenPublication` named `cliFatJar` in the `cli/build.gradle.kts` file.
     */
    public fun fatCli(version: String): String = "$group:compiler-cli-all:$version"

    /**
     * Obtains Maven coordinates for the Compiler command-line application.
     */
    public fun cli(version: String): String = "$group:compiler-cli:$version"

    /**
     * Obtains Maven coordinates for the Compiler plugin to Google Protobuf Compiler (`protoc`).
     */
    public fun protocPlugin(version: String): ProtocPluginArtifact = ProtocPluginArtifact(version)
}

/**
 * Holds Maven references to the `protoc` plugin artifact of the Compiler.
 *
 * Provided to treat this important dependency in a type-safe way.
 */
public data class ProtocPluginArtifact(val version: String) {

    /**
     * The Maven coordinates of the Compiler plugin for `protoc`.
     */
    public val coordinates: String =
        "${Artifacts.group}:compiler-protoc-plugin:$version:exe@jar"

    /**
     * Obtains Maven artifact coordinates.
     */
    override fun toString(): String = coordinates
}
