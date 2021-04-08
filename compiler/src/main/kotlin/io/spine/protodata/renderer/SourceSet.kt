/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.protodata.renderer

import com.google.common.collect.ImmutableSet.toImmutableSet
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

/**
 * A set of source files.
 */
@Suppress("DataClassPrivateConstructor")
    // It's OK once we have and instance to change it. Making constructor `private` here so that
    // users do not attempt re-building an object from scratch and use `fromContentsOf` or
    // `withFiles` instead.
public data class SourceSet
private constructor(val files: Set<SourceFile>, internal val rootDir: Path) {

    public companion object {

        /**
         * Collects a source set from a given root directory.
         */
        public fun fromContentsOf(directory: Path): SourceSet {
            val files = Files
                .walk(directory)
                .filter { it.isRegularFile() }
                .map { SourceFile.read(it) }
                .collect(toImmutableSet())
            return SourceSet(files, directory)
        }
    }

    /**
     * Looks up a file by its path.
     *
     * The [path] may be a relative or an absolute path the file.
     */
    public fun file(path: Path): SourceFile =
        files.find { it.path.endsWith(path) }
            ?: throw IllegalArgumentException("File not found: `$path`.")

    /**
     * Constructs a `SourceSet` in the same root directory out of the given source files.
     */
    public fun withFiles(files: Set<SourceFile>): SourceSet =
        SourceSet(files, rootDir)
}
