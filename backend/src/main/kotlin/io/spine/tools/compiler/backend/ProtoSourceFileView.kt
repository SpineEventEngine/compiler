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

package io.spine.tools.compiler.backend

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.ProtobufSourceFile
import io.spine.tools.compiler.ast.event.EnumDiscovered
import io.spine.tools.compiler.ast.event.FileEntered
import io.spine.tools.compiler.ast.event.ServiceDiscovered
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.plugin.View
import io.spine.server.entity.alter

/**
 * A view which collects information about a Protobuf source file.
 */
internal class ProtoSourceFileView
    : View<File, ProtobufSourceFile, ProtobufSourceFile.Builder>() {

    @Subscribe
    fun on(@External e: FileEntered) = alter {
        file = e.file
        header = e.header
    }

    @Subscribe
    fun on(@External e: TypeDiscovered) = alter {
        putType(e.type.typeUrl, e.type)
    }

    @Subscribe
    fun on(@External e: EnumDiscovered) = alter {
        putEnumType(e.type.typeUrl, e.type)
    }

    @Subscribe
    fun on(@External e: ServiceDiscovered) = alter{
        putService(e.service.typeUrl, e.service)
    }
}
