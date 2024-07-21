/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.java.file

import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import kotlin.io.path.extension

/**
 * Tells if this is a Java source file.
 */
public val SourceFile<*>.isJava: Boolean
    get() = relativePath.extension == "java"

/**
 * Tells if this source file set produces files that reside under the `java` directory.
 */
public val SourceFileSet.hasJavaRoot: Boolean
    get() = outputRoot.endsWith("java")

/**
 * Tells if this source file set produces files that reside under the `java` directory.
 */
@Deprecated(message = "Please use `hasJavaRoot` instead.", ReplaceWith("hasJavaRoot"))
public val SourceFileSet.hasJavaOutput: Boolean
    get() = hasJavaRoot

/**
 * Tells if this source file set produces files that reside under the `grpc` directory.
 */
public val SourceFileSet.hasGrpcRoot: Boolean
    get() = outputRoot.endsWith("grpc")

/**
 * Tells if this source file set produces files that reside under the `grpc` directory.
 */
@Deprecated(message = "Please use `hasGrpcRoot` instead.", ReplaceWith("hasGrpcRoot"))
public val SourceFileSet.hasGrpcOutput: Boolean
    get() = hasGrpcRoot

/**
 * Tells if this source file set has at least one Java file.
 */
public val SourceFileSet.hasJavaFiles: Boolean
    get() = any { it.isJava }
