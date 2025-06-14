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

package io.spine.tools.compiler.jvm.style

import com.intellij.psi.codeStyle.JavaCodeStyleSettings
import io.spine.tools.compiler.jvm.render.JavaRenderer
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.compiler.render.SourceFileSet
import io.spine.tools.compiler.render.forEachOfLanguage
import io.spine.tools.compiler.settings.defaultConsumerId
import io.spine.tools.compiler.settings.loadSettings
import io.spine.tools.code.Java
import io.spine.tools.psi.codeStyleManager
import io.spine.tools.psi.codeStyleSettings
import io.spine.tools.psi.content
import io.spine.tools.psi.convertLineSeparators
import io.spine.tools.psi.force
import io.spine.tools.psi.get
import io.spine.tools.psi.java.Environment
import io.spine.tools.psi.java.Parser
import io.spine.tools.psi.java.execute

/**
 * Reformats Java source code files using settings passed as [JavaCodeStyle] instance.
 *
 * If no settings are passed, default Java code style settings used in Spine SDK are applied.
 * 
 * @see javaCodeStyleDefaults
 */
public class JavaCodeStyleFormatter : JavaRenderer() {

    override val consumerId: String = settingsId

    private val project = Environment.project

    private val codeStyle: JavaCodeStyle by lazy {
        if (settingsAvailable()) {
            loadSettings<JavaCodeStyle>()
        } else {
            javaCodeStyleDefaults()
        }
    }

    private var appliedToIntelliJ: Boolean = false

    private fun applyStyleToIntelliJ() {
        if (!appliedToIntelliJ) {
            val ijCodeStyleSettings = project.codeStyleSettings
            val ijJavaCodeStyleSettings = ijCodeStyleSettings.get<JavaCodeStyleSettings>()
            codeStyle.run {
                applyTo(ijCodeStyleSettings)
                applyTo(ijJavaCodeStyleSettings)
            }
            project.force(ijCodeStyleSettings)
            appliedToIntelliJ = true
        }
    }

    private val parser by lazy {
        Parser(project)
    }

    override fun render(sources: SourceFileSet) {
        applyStyleToIntelliJ()
        sources.forEachOfLanguage<Java> {
            reformat(it)
        }
    }

    private fun reformat(file: SourceFile<Java>) {
        val withAdjustedSeparators = file.code().convertLineSeparators()
        val outputFile = file.outputPath.toFile()
        val psiFile = parser.parse(withAdjustedSeparators, outputFile)
        execute {
            project.codeStyleManager.reformat(psiFile)
        }
        val updatedCode = psiFile.content()
        file.overwrite(updatedCode)
    }

    public companion object {

        /**
         * The ID to be used when passing settings to [JavaCodeStyleFormatter].
         */
        public val settingsId: String = JavaCodeStyle::class.java.defaultConsumerId
    }
}
