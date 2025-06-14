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

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.protobuf.AnyPacker
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.event.FieldOptionDiscovered
import io.spine.tools.compiler.cli.test.DefaultOptionsCounter
import io.spine.tools.compiler.plugin.View
import io.spine.server.entity.alter
import io.spine.server.route.Route
import io.spine.time.validation.Time
import io.spine.time.validation.TimeOption

/**
 * Records the processing of options defined for `SpineAnnotatedType` in `test.proto`
 *
 * The options are defined in Spine-default `options.proto` and `time_options.proto`.
 * The corresponding test case in `CliTest` checks that these Proto options are provided
 * to the Compiler by default.
 */
class DefaultOptionsCounterView
    : View<TypeName, DefaultOptionsCounter, DefaultOptionsCounter.Builder>() {

    @Subscribe
    internal fun onWhen(
        @External @Where(field = "option.name", equals = "when")
        event: FieldOptionDiscovered
    ) = alter {
        timestampInFutureEncountered = readTimeOption(event).`in`.equals(Time.FUTURE)
    }

    @Subscribe
    internal fun onRequired(
        @External @Where(field = "option.name", equals = "required")
        event: FieldOptionDiscovered
    ) = alter {
        requiredFieldForTestEncountered = requiredFieldForTestEncountered ||
                event.subject.name.value.equals("required_field_for_test")
    }

    private fun readTimeOption(event: FieldOptionDiscovered): TimeOption =
        AnyPacker.unpack(event.option.value, TimeOption::class.java)

    companion object {

        @Route
        @JvmStatic
        fun route(e: FieldOptionDiscovered): TypeName = e.subject.declaringType
    }
}
