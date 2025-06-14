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

package io.spine.tools.compiler.cli.given

import io.spine.tools.compiler.cli.test.DefaultOptionsCounter
import io.spine.tools.compiler.plugin.Plugin
import io.spine.tools.compiler.render.SourceFileSet
import io.spine.tools.compiler.test.StubSoloRenderer
import io.spine.server.query.Querying
import io.spine.server.query.select
import kotlin.io.path.Path

class DefaultOptionsCounterRenderer : StubSoloRenderer() {

    companion object {
        const val FILE_NAME = "default_opts_counted.txt"
    }

    override fun render(sources: SourceFileSet) {
        val counters = (this as Querying).select<DefaultOptionsCounter>().all()
        sources.createFile(
            Path(FILE_NAME),
            counters.joinToString(separator = ",") {
                it.timestampInFutureEncountered.toString() + ", " +
                        it.requiredFieldForTestEncountered.toString()
            }
        )
    }
}
class DefaultOptionsCounterRendererPlugin : Plugin(
    renderers = listOf(DefaultOptionsCounterRenderer())
)
