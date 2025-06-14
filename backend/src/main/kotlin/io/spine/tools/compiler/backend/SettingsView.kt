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
import io.spine.tools.compiler.ast.nameWithoutExtension
import io.spine.tools.compiler.plugin.View
import io.spine.tools.compiler.settings.Settings
import io.spine.tools.compiler.settings.event.SettingsFileDiscovered
import io.spine.server.entity.alter
import io.spine.server.route.Route

/**
 * A view on the Compiler user configuration.
 *
 * Can contain either a configuration file path or a string value of the configuration.
 *
 * @see io.spine.tools.compiler.settings.WithSettings for fetching the value of the user configuration.
 */
internal class SettingsView : View<String, Settings, Settings.Builder>() {

    @Subscribe
    internal fun on(@External event: SettingsFileDiscovered) = alter {
        file = event.file
    }

    companion object {

        @Route
        @JvmStatic
        fun route(e: SettingsFileDiscovered): String = e.file.nameWithoutExtension
    }
}
