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

@file:JvmName("Plugin")

package io.spine.tools.compiler.protoc

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import io.spine.code.proto.CodeGeneratorRequestWriter

/**
 * Stores received `CodeGeneratorRequest` message to the file the name of which is passed as
 * the value of the [parameter][CodeGeneratorRequest.getParameter] property of the request.
 *
 * The name of the file is [Base64][java.util.Base64] encoded.
 *
 * The function returns empty [CodeGeneratorRequest] written to [System.out]
 * according to the `protoc` plugin
 * [protocol](https://protobuf.dev/reference/cpp/api-docs/google.protobuf.compiler.plugin.pb/).
 *
 * @see CodeGeneratorRequestWriter
 */
public fun main() {
    val writer = CodeGeneratorRequestWriter(System.`in`)
    writer.writeBinary()
    writer.writeJson()
    val emptyResponse = CodeGeneratorResponse.getDefaultInstance()
    emptyResponse.writeTo(System.out)
}
