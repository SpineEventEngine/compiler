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

package io.spine.tools.compiler.settings

import io.spine.format.Format
import io.spine.format.hasSupportedFormat
import io.spine.tools.compiler.ast.toAbsoluteFile
import io.spine.tools.compiler.settings.event.SettingsFileDiscovered
import io.spine.tools.compiler.settings.event.settingsFileDiscovered
import io.spine.tools.compiler.util.ensureExistingDirectory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

/**
 * A directory containing settings files for the Compiler plugins or their parts.
 *
 * Only the files with the [recognized extensions][Format] are considered settings files.
 *
 * Only the files belonging to the directory directly are considered,
 * no subdirectories are traversed.
 *
 * This class works in combination with the view which manages the [Settings] state class
 * on the Compiler backend. The view is subscribed to the [SettingsFileDiscovered] event for
 * storing the name of the discovered settings file.
 *
 * The method [emitEvents] of this class traverses all the files stored in the settings directory
 * emitting the [SettingsFileDiscovered] event for each file with the recognized format.
 *
 * In order to load settings, Compiler plugins or their parts should implement
 * the [LoadsSettings] interface, which provides the discovery and loading methods based
 * on querying instances of the [Settings] class on the backend.
 *
 * @param path The existing path to the settings directory.
 */
public class SettingsDirectory(
    public val path: Path
) {
    /**
     * Writes a settings file for the given consumer.
     *
     * @param consumerId The ID of the consumer to write settings for.
     * @param format The format of the settings file.
     * @param content The content of the settings file.
     */
    public fun write(consumerId: String, format: Format<*>, content: String) {
        val file = file(consumerId, format)
        ensureExistingDirectory(path)
        file.writeText(content)
    }

    /**
     * Writes a settings file for the consumer specified by the generic parameter.
     *
     * @param T The type of the settings consumer.
     * @param format The format of the settings file.
     * @param content The content of the settings file.
     */
    public inline fun <reified T : LoadsSettings> writeFor(
        format: Format<*>,
        content: String
    ) {
        write(T::class.java.defaultConsumerId, format, content)
    }

    /**
     * Writes a settings file for the given consumer.
     *
     * @param consumerId The ID of the consumer to write settings for.
     * @param format The format of the settings file.
     * @param content The content of the settings file.
     */
    public fun write(consumerId: String, format: Format<*>, content: ByteArray) {
        val file = file(consumerId, format)
        ensureExistingDirectory(path)
        file.writeBytes(content)
    }

    /**
     * Writes a settings file for the consumer specified by the generic parameter.
     *
     * @param T The type of the settings consumer.
     * @param format The format of the settings file.
     * @param content The content of the settings file.
     */
    public inline fun <reified T : LoadsSettings> writeFor(
        format: Format<*>,
        content: ByteArray
    ) {
        write(T::class.java.defaultConsumerId, format, content)
    }

    private fun file(consumerId: String, format: Format<*>): File {
        val fileName = "${consumerId}.${format.extensions.first()}"
        return path.resolve(fileName).toFile()
    }

    /**
     * Traverses the setting files in this directory and emits
     * [SettingsFileDiscovered] for each of them.
     */
    public fun emitEvents(): List<SettingsFileDiscovered> =
        files().map {
            settingsFileDiscovered {
                file = it.toAbsoluteFile()
            }
        }

    private fun files(): List<Path> =
        if (path.exists()) {
            path.listDirectoryEntries()
                .filter { it.toFile().hasSupportedFormat() }
        } else {
            emptyList()
        }
}
