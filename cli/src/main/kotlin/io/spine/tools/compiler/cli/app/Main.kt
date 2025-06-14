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

package io.spine.tools.compiler.cli.app

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import io.spine.tools.compiler.backend.Pipeline
import io.spine.tools.compiler.params.DebugLoggingParam
import io.spine.tools.compiler.params.InfoLoggingParam
import io.spine.tools.compiler.params.Parameter
import io.spine.tools.compiler.params.ParametersFileParam
import io.spine.format.parse
import io.spine.logging.Level
import io.spine.logging.WithLogging
import io.spine.logging.context.LogLevelMap
import io.spine.logging.context.ScopedLoggingContext
import io.spine.string.Separator
import io.spine.string.qualifiedClassName
import io.spine.tools.code.manifest.Version
import io.spine.tools.compiler.params.PipelineParameters
import java.io.File
import kotlin.system.exitProcess

/**
 * Launches the CLI application.
 *
 * When the application is done exists the process with the code `0`.
 * If an unhandled error occurs, exits the process with the code `-1`.
 */
@Suppress(
    "TooGenericExceptionCaught", // We do want the most generic type thrown.
    "PrintStackTrace"
)
public fun main(args: Array<String>) {
    try {
        val version = readVersion()
        val run = Run(version)
        run.main(args)
        exitProcess(0)
    } catch (e: Throwable) {
        System.err.run {
            println("`${e.qualifiedClassName}` caught in Spine Compiler `main()`:")
            println("Message: ${e.message}")
            println("Stacktrace:")
            println("```")
            e.printStackTrace(this)
            println("```")
        }
        exitProcess(-1)
    }
}

private fun readVersion(): String = Version.fromManifestOf(Run::class.java).value

/**
 * The main CLI command which performs the Compiler code generation tasks.
 */
@Suppress("TooManyFunctions") // It is OK for the `main` entry point.
internal class Run(version: String) : CliktCommand(
    name = "spine",
    help = "The Spine Compiler helps build better multi-platform code generation." +
            Separator.nl() +
            "Version $version.",
    epilog = "https://github.com/SpineEventEngine/compiler/",
    printHelpOnEmptyArgs = true
), WithLogging {

    private fun Parameter.toOption(cc: CompletionCandidates? = null) = option(
        name, shortName,
        help = help,
        completionCandidates = cc
    )

    private val paramsFile: File by ParametersFileParam.toOption().file(
        mustExist = true,
        canBeDir = false,
        canBeSymlink = false,
        mustBeReadable = true
    ).required()

    private val debug: Boolean by DebugLoggingParam.toOption().flag(default = false)

    private val info: Boolean by InfoLoggingParam.toOption().flag(default = false)

    private val loggingLevel: Level by lazy {
        checkUsage(!(debug && info)) {
            "Debug and info logging levels cannot be enabled at the same time."
        }
        when {
            debug -> Level.DEBUG
            info -> Level.INFO
            else -> Level.WARNING
        }
    }

    override fun run() {
        if (loggingLevel == Level.WARNING) {
            doRun()
        } else {
            val logLevelMap = LogLevelMap.create(mapOf(), loggingLevel)
            val context = ScopedLoggingContext.newContext().withLogLevelMap(logLevelMap)
            context.execute {
                doRun()
            }
        }
    }

    private fun doRun() {
        val params = parse<PipelineParameters>(paramsFile)
        val pipeline = Pipeline(params = params)
        pipeline()
    }
}

/**
 * Throws an [UsageError] with the result of calling [lazyMessage] if the [condition] isn't met.
 */
private inline fun checkUsage(condition: Boolean, lazyMessage: () -> Any) {
    if (condition.not()) {
        val message = lazyMessage()
        throw UsageError(message.toString())
    }
}
