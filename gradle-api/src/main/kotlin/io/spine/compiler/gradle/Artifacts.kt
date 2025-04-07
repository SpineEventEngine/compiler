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

package io.spine.compiler.gradle

/**
 * Constants for locating ProtoData in Maven repositories.
 */
@Suppress("ConstPropertyName") // https://bit.ly/kotlin-prop-names
public object Artifacts {

    /**
     * The Maven group of the ProtoData artifacts.
     */
    public const val group: String = "io.spine.compiler"

    /**
     * The name of the Spine Compiler artifact.
     */
    public const val compiler: String = "compiler"

    /**
     * Obtains Maven coordinates of the `fat-cli` variant of command-line application.
     *
     * "fat-cli" is an all-in-one distribution of the Compiler, published somewhat in the past.
     * Ironically, we need it in the Compiler development.
     * It removes the dependency conflicts between Compiler-s.
     *
     * Please make sure that the module name in this constant (`cli-all`) has the same value
     * as the `MavenPublication` named `cliFatJar` in the `cli/build.gradle.kts` file.
     */
    public fun fatCli(version: String): String = "$group:cli-all:$version"

    /**
     * Obtains Maven coordinates for ProtoData command-line application.
     */
    public fun cli(version: String): String = "$group:cli:$version"

    /**
     * Obtains Maven coordinates for the ProtoData plugin to Google Protobuf Compiler (`protoc`).
     */
    public fun protocPlugin(version: String): ProtocPluginArtifact = ProtocPluginArtifact(version)
}

/**
 * Holds Maven references to `protoc` plugin artifact of ProtoData.
 *
 * Provided to treat this important dependency in a type-safe way.
 */
public data class ProtocPluginArtifact(val version: String) {

    /**
     * The Maven cooridates of the Spine Compiler plugin for `protoc`.
     */
    public val coordinates: String =
        "${Artifacts.group}:protoc-plugin:$version:exe@jar"

    /**
     * Obtains Maven artifact coordinates.
     */
    override fun toString(): String = coordinates
}
