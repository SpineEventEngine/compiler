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

package io.spine.protodata.plugin

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.core.External
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.ast.event.TypeEntered
import io.spine.protodata.type.TypeSystem
import io.spine.server.event.Just
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.tuple.EitherOf2
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`Policy` should")
internal class PolicySpec {

    @Test
    fun `obtain 'TypeSystem' after injected`() {
        val policy = StubPolicy()

        policy.typeSystem() shouldBe null

        val typeSystem = TypeSystem(emptySet())
        policy.use(typeSystem)

        policy.typeSystem() shouldBe typeSystem
    }

    /**
     * This test merely makes the [Policy.ignore] method used without making any
     * meaningful assertions.
     *
     * It creates a [Policy] which calls the `protected` method of the companion object
     * showing the usage scenario.
     *
     * @see PolicyJavaApiSpec.allowIgnoring the test for Java API.
     */
    @Test
    @JvmName("allowIgnoring")
    fun `have a shortcut for ignoring incoming events`() {
        val policy = object : Policy<TypeEntered>() {
            @React
            override fun whenever(
                @External event: TypeEntered
            ): EitherOf2<TypeEntered, NoReaction> = ignore()
        }
        policy shouldNotBe null
    }
}

private class StubPolicy : TsStubPolicy<TypeDiscovered>() {
    @React
    override fun whenever(@External event: TypeDiscovered): Just<NoReaction> = Just.noReaction
}
