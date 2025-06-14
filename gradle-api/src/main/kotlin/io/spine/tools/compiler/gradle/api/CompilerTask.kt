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

package io.spine.tools.compiler.gradle.api

import io.spine.tools.code.SourceSetName
import io.spine.tools.gradle.task.TaskWithSourceSetName
import io.spine.tools.compiler.gradle.api.Names.TASK_PREFIX
import io.spine.tools.compiler.gradle.api.Names.TASK_SUFFIX
import org.gradle.api.tasks.SourceSet

/**
 * Utilities for working with `launchSpineCompiler` tasks in a Gradle project.
 */
public object CompilerTask : TaskLocator() {

    /**
     * Obtains a name of the task for the given source set.
     */
    override fun nameFor(sourceSet: SourceSet): String {
        val ssn = SourceSetName(sourceSet.name)
        return CompilerTaskName(ssn).value()
    }
}

/**
 * The name of the `LaunchSpineCompiler` task for the given source set.
 */
public class CompilerTaskName(ssn: SourceSetName) :
    TaskWithSourceSetName("$TASK_PREFIX${ssn.toInfix()}$TASK_SUFFIX", ssn)
