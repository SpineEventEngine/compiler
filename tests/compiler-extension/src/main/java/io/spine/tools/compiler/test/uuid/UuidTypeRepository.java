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

package io.spine.tools.compiler.test.uuid;

import io.spine.tools.compiler.ast.event.FieldEntered;
import io.spine.tools.compiler.ast.TypeName;
import io.spine.tools.compiler.plugin.ViewRepository;
import io.spine.tools.compiler.test.UuidType;
import io.spine.server.route.EventRouting;
import org.jspecify.annotations.NonNull;

import static io.spine.server.route.EventRoute.withId;

/**
 * The repository for the {@link UuidType} views.
 *
 * <p>Configures routing for {@code FieldEntered} events.
 */
final class UuidTypeRepository extends ViewRepository<TypeName, UuidTypeView, UuidType> {

    @Override
    protected void setupEventRouting(@NonNull EventRouting<TypeName> routing) {
        super.setupEventRouting(routing);
        routing.route(FieldEntered.class,
                      (message, context) -> withId(message.getType()));
    }
}
