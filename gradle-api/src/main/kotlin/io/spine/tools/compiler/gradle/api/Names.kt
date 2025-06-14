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

/**
 * The name of various objects in Spine Compiler Gradle API.
 */
public object Names {

    /**
     * The ID of the Protobuf Gradle Plugin.
     */
    public const val PROTOBUF_GRADLE_PLUGIN_ID: String = "com.google.protobuf"

    /**
     * The name of the `protoc` plugin exposed by the Compiler.
     */
    public const val SPINE_COMPILER_PROTOC_PLUGIN: String = "spine"

    /**
     * The ID of the Compiler Gradle plugin.
     */
    public const val GRADLE_PLUGIN_ID: String = "io.spine.compiler"

    /**
     * The name of the Gradle extension added by the Compiler Gradle plugin.
     */
    public const val EXTENSION_NAME: String = "compiler"

    /**
     * The name of the Gradle Configuration created by Compiler Gradle plugin
     * for holding user-defined classpath.
     */
    public const val USER_CLASSPATH_CONFIGURATION: String = "spineCompiler"

    /**
     * The name of the configuration containing the Compiler `fat-cli` artifact.
     */
    public const val COMPILER_RAW_ARTIFACT: String = "spineCompilerRawArtifact"

    /**
     * Gradle task prefix.
     */
    public const val TASK_PREFIX: String = "launch"

    /**
     * Gradle task suffix.
     */
    public const val TASK_SUFFIX: String = "SpineCompiler"
}
