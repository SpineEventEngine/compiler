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

package io.spine.tools.compiler.protobuf

import com.google.protobuf.Descriptors.MethodDescriptor
import io.spine.tools.compiler.ast.CallCardinality
import io.spine.tools.compiler.ast.CallCardinality.BIDIRECTIONAL_STREAMING
import io.spine.tools.compiler.ast.CallCardinality.CLIENT_STREAMING
import io.spine.tools.compiler.ast.CallCardinality.SERVER_STREAMING
import io.spine.tools.compiler.ast.CallCardinality.UNARY
import io.spine.tools.compiler.ast.Rpc
import io.spine.tools.compiler.ast.RpcName
import io.spine.tools.compiler.ast.ServiceName
import io.spine.tools.compiler.ast.coordinates
import io.spine.tools.compiler.ast.copy
import io.spine.tools.compiler.ast.documentation
import io.spine.tools.compiler.ast.options
import io.spine.tools.compiler.ast.rpc
import io.spine.tools.compiler.ast.rpcName

/**
 * Obtains the name of this RPC method as an [RpcName].
 */
public fun MethodDescriptor.name(): RpcName = rpcName { value = name }

/**
 * Converts this method descriptor into an [Rpc] with options.
 *
 * @see buildRpc
 */
public fun MethodDescriptor.toRpc(declaringService: ServiceName): Rpc {
    val rpc = buildRpc(this, declaringService)
    return rpc.copy {
        option.addAll(options())
    }
}

/**
 * Obtains the [CallCardinality] of this RPC method.
 *
 * The cardinality determines how many messages may flow from the client to the server and back.
 */
public val MethodDescriptor.cardinality: CallCardinality
    get() = when {
        !isClientStreaming && !isServerStreaming -> UNARY
        !isClientStreaming && isServerStreaming -> SERVER_STREAMING
        isClientStreaming && !isServerStreaming -> CLIENT_STREAMING
        isClientStreaming && isServerStreaming -> BIDIRECTIONAL_STREAMING
        else -> error("Unable to determine cardinality of method: `$fullName`.")
    }

/**
 * Converts this method descriptor into an [Rpc].
 *
 * The resulting [Rpc] will not reflect the method options.
 *
 * @see toRpc
 */
public fun buildRpc(
    desc: MethodDescriptor,
    declaringService: ServiceName
): Rpc = rpc {
    name = desc.name()
    cardinality = desc.cardinality
    requestType = desc.inputType.name()
    responseType = desc.outputType.name()
    service = declaringService
    val serviceDescr = desc.service
    doc = serviceDescr.documentation().forRpc(desc)
    span = serviceDescr.coordinates().forRpc(desc)
}
