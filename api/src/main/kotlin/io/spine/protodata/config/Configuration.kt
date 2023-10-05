/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata.config

import io.spine.annotation.Internal
import io.spine.base.EventMessage
import io.spine.protodata.config.event.FileConfigDiscovered
import io.spine.protodata.config.event.RawConfigDiscovered
import io.spine.protodata.config.event.fileConfigDiscovered
import io.spine.protodata.config.event.rawConfigDiscovered
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * User-provided custom configuration for ProtoData.
 *
 * @see Configured
 */
@Internal
public sealed class Configuration {

    /**
     * Constructs an event which contains the value of the configuration.
     *
     * The event belongs to [ConfigurationContext][io.spine.protodata.backend.ConfigurationContext].
     */
    public abstract fun produceEvent(): EventMessage

    public companion object {

        /**
         * Creates a configuration written in a file with the given path.
         */
        public fun file(file: Path): Configuration = File(file)

        /**
         * Creates a configuration from the given value in the given format.
         */
        public fun rawValue(value: String, format: ConfigurationFormat): Configuration =
            Raw(value, format)
    }
}

/**
 * A [Configuration] consisting of one [ConfigFile].
 *
 * Produces [FileConfigDiscovered] event upon discovery.
 */
private class File(private val file: Path) : Configuration() {

    override fun produceEvent(): FileConfigDiscovered = fileConfigDiscovered {
        file = this@File.file.toConfigFile()
    }

    private fun Path.toConfigFile() = configFile {
        path = absolutePathString()
    }
}

/**
 * A [Configuration] produced from a [value] in the specified [format].
 *
 * Produces [RawConfigDiscovered] event upon discovery.
 */
private class Raw(
    private val value: String,
    private val format: ConfigurationFormat
) : Configuration() {

    override fun produceEvent(): RawConfigDiscovered = rawConfigDiscovered {
        config = config()
    }

    private fun config() = rawConfig {
        format = this@Raw.format
        value = this@Raw.value
    }
}
