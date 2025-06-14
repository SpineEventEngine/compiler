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

package io.spine.tools.compiler.jvm.annotation

import com.google.common.annotations.VisibleForTesting
import given.annotation.NoTypeTargetAnnotation
import given.annotation.Schedule
import io.kotest.matchers.shouldBe
import io.spine.logging.testing.tapConsole
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.java.reference
import java.nio.file.Paths
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`ClassAnnotationRenderer` should")
internal class TypeAnnotationSpec {

    @Test
    fun `reject annotation class without 'TYPE' target`() {
        assertThrows<IllegalArgumentException> {
            StubAnnotation(NoTypeTargetAnnotation::class.java)
        }
    }

    @Test
    fun `reject simultaneously 'subject' and 'file' arguments`() {
        assertThrows<IllegalArgumentException> {
            @Suppress("UNCHECKED_CAST") // Ensured by the file extension.
            StubAnnotation(SuppressWarnings::class.java,
                ClassName("foo", "bar"),
                SourceFile.fromCode(Paths.get("stub", "Source.java"), """
                    class Source { }    
                    """.trimIndent()
                ) as SourceFile<Java>
            )
        }
    }

    @Test
    fun `accept annotation class with 'TYPE' target`() {
        assertDoesNotThrow {
            StubAnnotation(SuppressWarnings::class.java)
        }
    }

    @Nested inner class
    `for non-repeatable annotation class` {

        private val annotation = StubAnnotation(annotationClass)

        @Test
        fun `do not annotate if it already present`() {
            val file = fileWithAnnotation
            annotation.shouldAnnotate(file) shouldBe false

            annotation.annotate(file)
            file.countAnnotations(annotationClass.reference) shouldBe 1
        }

        @Test
        fun `annotate if is not present`() {
            val file = fileWithoutAnnotation

            annotation.shouldAnnotate(file) shouldBe true

            annotation.annotate(file)
            file.countAnnotations(annotationClass.reference) shouldBe 1
        }
    }

    @Nested inner class
    `for repeatable annotation class, annotate if the annotation` {

        private val annotation = StubAnnotation(repeatableAnnotationClass)

        @Test
        fun `is already present`() {
            annotation.shouldAnnotate(fileWithRepeatableAnnotation) shouldBe true
        }

        @Test
        fun `is absent`() {
            annotation.shouldAnnotate(fileWithoutAnnotation) shouldBe true
        }
    }

    @Test
    fun `reject files without 'BeforePrimaryDeclaration' insertion point`() {
        tapConsole {
            val annotation = StubAnnotation(repeatableAnnotationClass)
            annotation.shouldAnnotate(fileWithoutInsertionPoint) shouldBe false
        }
    }

    @Test
    fun `tell it should annotate a nested type`() {
        val className = ClassName(PACKAGE_NAME, "TheOuterClass", "NestedClassToAnnotate")
        val annotation = StubAnnotation(annotationClass, className)
        annotation.shouldAnnotate(fileWithNestedType) shouldBe true
    }
}

private class StubAnnotation<T : Annotation>(
    annotationClass: Class<T>,
    subject: ClassName? = null,
    file: SourceFile<Java>? = null
) :
    TypeAnnotation<T>(annotationClass, subject, file) {

    override fun renderAnnotationArguments(file: SourceFile<Java>): String = ""

    /**
     * Opens access for tests.
     */
    @VisibleForTesting
    public override fun shouldAnnotate(file: SourceFile<Java>): Boolean = super.shouldAnnotate(file)
}

private fun SourceFile<Java>.countAnnotations(clsName: String): Int =
    lines().count { it.contains(clsName) }

/**
 * A stable path to the Java source code file.
 */
private val path = Paths.get("given", "java", "code", "TheClassToAnnotate.java")

private const val PACKAGE_NAME = "given.java.code"

private val annotationClass = SuppressWarnings::class.java

private val repeatableAnnotationClass = Schedule::class.java

/**
 * A source file with a non-repeatable annotation.
 */
@Suppress("UNCHECKED_CAST") // Ensured by the file extension.
private val fileWithAnnotation: SourceFile<Java>
    get() = SourceFile.fromCode(
    code = """
        package $PACKAGE_NAME;
        
        /* INSERT:'BeforePrimaryDeclaration' */

        @${annotationClass.reference}("SomeWarning")
        public class TheClassToAnnotate {
          // Empty by design. 
        }
    """.trimIndent(),
    relativePath = path
) as SourceFile<Java>

@Suppress("UNCHECKED_CAST") // Ensured by the file extension.
private val fileWithoutAnnotation: SourceFile<Java>
    get() = SourceFile.fromCode(
    code = """
        package $PACKAGE_NAME;
        
        /* INSERT:'BeforePrimaryDeclaration' */

        public class TheClassToAnnotate {
          // Empty by design. 
        }
    """.trimIndent(),
    relativePath = path
) as SourceFile<Java>

@Suppress("UNCHECKED_CAST") // Ensured by the file extension.
private val fileWithRepeatableAnnotation: SourceFile<Java>
    get() = SourceFile.fromCode(
    code = """
        package $PACKAGE_NAME;
        
        /* INSERT:'BeforePrimaryDeclaration' */

        @given.annotation.Schedule
        public class TheClassToAnnotate {
          // Empty by design. 
        }
    """.trimIndent(),
    relativePath = path
) as SourceFile<Java>

@Suppress("UNCHECKED_CAST") // Ensured by the file extension.
private val fileWithoutInsertionPoint: SourceFile<Java>
    get() = SourceFile.fromCode(
    code = """
        /**
         * A Java file without {@code BeforePrimaryDeclaration} insertion point.
         */
        package $PACKAGE_NAME;
    """.trimIndent(),
    relativePath = Paths.get("given", "java", "code", "package-info.java")
) as SourceFile<Java>

@Suppress("UNCHECKED_CAST") // Ensured by the file extension.
private val fileWithNestedType: SourceFile<Java>
    get() = SourceFile.fromCode(
    code = """
        package $PACKAGE_NAME;
        
        /* INSERT:'BeforePrimaryDeclaration' */

        public class TheOuterClass {
        
          /** Stub Javadoc text. */
          public class NestedClassToAnnotate {
            // Empty by design. 
          }
        }
    """.trimIndent(),
    relativePath = path
) as SourceFile<Java>
