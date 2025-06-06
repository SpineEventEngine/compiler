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

package io.spine.tools.compiler.backend.event

import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import io.spine.base.EventMessage
import io.spine.tools.compiler.ast.ProtoFileHeader
import io.spine.tools.compiler.ast.event.ServiceEntered
import io.spine.tools.compiler.ast.event.ServiceExited
import io.spine.tools.compiler.ast.event.rpcEntered
import io.spine.tools.compiler.ast.event.rpcExited
import io.spine.tools.compiler.ast.event.rpcOptionDiscovered
import io.spine.tools.compiler.ast.event.serviceDiscovered
import io.spine.tools.compiler.ast.event.serviceEntered
import io.spine.tools.compiler.ast.event.serviceExited
import io.spine.tools.compiler.ast.event.serviceOptionDiscovered
import io.spine.tools.compiler.ast.produceOptionEvents
import io.spine.tools.compiler.ast.withAbsoluteFile
import io.spine.tools.compiler.protobuf.buildRpc
import io.spine.tools.compiler.protobuf.name
import io.spine.tools.compiler.protobuf.toService

/**
 * Produces events for a service.
 */
internal class ServiceEvents(header: ProtoFileHeader) :
    DeclarationEvents<ServiceDescriptor>(header) {

    /**
     * Yields events for the given service.
     *
     * Opens with an [ServiceEntered] event.
     * Then go the events regarding the service metadata.
     * Then go the events regarding the RPC methods.
     * At last, closes with an [ServiceExited] event.
     */
    override suspend fun SequenceScope<EventMessage>.produceEvents(
        desc: ServiceDescriptor
    ) {
        val path = header.file
        val serviceType = desc.toService().withAbsoluteFile(path)
        yield(
            serviceDiscovered {
                file = path
                service = serviceType
            }
        )
        val serviceName = desc.name()
        yield(
            serviceEntered {
                file = path
                service = serviceName
            }
        )
        produceOptionEvents(desc.options, desc) {
            serviceOptionDiscovered {
                file = path
                subject = serviceType
                option = it
            }
        }
        desc.methods.forEach {
            produceRpcEvents(it)
        }
        yield(
            serviceExited {
                file = path
                service = serviceName
            }
        )
    }

    private suspend fun SequenceScope<EventMessage>.produceRpcEvents(
        desc: MethodDescriptor
    ) {
        val serviceName = desc.service.name()
        val path = header.file
        val theRpc = buildRpc(desc, serviceName)
        yield(
            rpcEntered {
                file = path
                service = serviceName
                rpc = theRpc
            }
        )
        produceOptionEvents(desc.options, desc) {
            rpcOptionDiscovered {
                file = path
                subject = theRpc
                option = it
            }
        }
        yield(
            rpcExited {
                file = path
                service = serviceName
                rpc = theRpc.name
            }
        )
    }
}
