/*
 * Copyright 2023, TeamDev. All rights reserved.
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
package io.spine.protodata.codegen.java.annotation

import io.spine.protodata.codegen.java.ClassOrEnumName
import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.codegen.java.file.BeforeNestedTypeDeclaration
import io.spine.protodata.codegen.java.file.BeforePrimaryDeclaration
import io.spine.protodata.codegen.java.isJavaLang
import io.spine.protodata.renderer.CoordinatesFactory.Companion.nowhere
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import java.lang.annotation.ElementType.TYPE
import java.lang.annotation.Target
import org.jetbrains.annotations.VisibleForTesting

/**
 * A [JavaRenderer] which annotates a Java type using the given [annotation][annotationClass].
 *
 * @param T the type of the annotation.
 */
public abstract class TypeAnnotation<T : Annotation>(

    /**
     * The class of the annotation to apply.
     */
    protected val annotationClass: Class<T>,

    /**
     * The class or enum class name to be annotated.
     *
     * Cannot be used together with [file].
     * If both [subject] and [file] are specified, `IllegalArgumentException` will be thrown.
     *
     * If both [subject] and [file] are `null`, the annotation is applied to all the classes
     * passed to this annotation.
     *
     * If not `null`, the annotation is applied only to the types specified by this field.
     */
    protected val subject: ClassOrEnumName? = null,

    /**
     * The source file top class of which should be annotated.
     *
     * Cannot be used together with [subject].
     * If both [file] and [subject] are specified, `IllegalArgumentException` will be thrown.
     *
     * If not `null`, the annotation is applied only to the top class of the specified file.
     */
    protected val file: SourceFile? = null
) : JavaRenderer() {

    private val specific: Boolean = subject != null || file != null

    init {
        @Suppress("LeakingThis")
        /* We call an overridable method from the constructor here.
           We need this method to be overridable because we're disabling this check
           for one child class (`SuppressWarningsAnnotation`).
           Please see the documentation of [SuppressWarningsAnnotation.checkAnnotationClass]
           for more details. Since the method is `internal`, and not `protected` or `public`,
           we should be fine.
        */
        checkAnnotationClass()

        require(!(subject != null && file != null)) {
            "Cannot specify both `subject` and `file`."
        }
    }

    final override fun render(sources: SourceFileSet) {
        if (!specific) {
            annotateMany(sources)
        } else {
            val file = this.file ?: subjectFileIn(sources)
            if (shouldAnnotate(file)) {
                annotate(file)
            }
        }
    }

    private fun subjectFileIn(sources: SourceFileSet): SourceFile {
        val file = sources.find(subject!!.javaFile)
        check(file != null) {
            "Cannot find a file for the type `${subject.canonical}`."
        }
        return file
    }

    private fun annotateMany(sources: SourceFileSet) =
        sources.forEach {
            annotate(it)
        }

    @VisibleForTesting
    internal fun annotate(file: SourceFile) {
        if (shouldAnnotate(file)) {
            val line = file.at(insertionPoint())
            val annotationCode = annotationCode(file)
            line.add(annotationCode)
        }
    }

    private fun insertionPoint() =
        if (subject == null) {
            BeforePrimaryDeclaration
        } else {
            if (subject.isNested) {
                BeforeNestedTypeDeclaration(subject)
            } else {
                BeforePrimaryDeclaration
            }
        }

    private fun annotationCode(file: SourceFile): String =
        "@${annotationClass.codeReference}${annotationArguments(file)}"

    /**
     * Specifies whether to annotate a given file using the caller's implementation.
     *
     * By default, the method checks whether the target file already includes the annotation.
     *
     * If a [Repeatable] annotation is attached to the annotation class,
     * it always applies the annotation and returns `true`.
     *
     * If file does not contain a [BeforePrimaryDeclaration] insertion point,
     * it returns `false`.
     *
     * If the insertion point exists, it checks the presence of the annotation.
     * If the annotation already exists, it returns `false`, `true` otherwise.
     */
    @Suppress("ReturnCount") // Cannot go lower here.
    protected open fun shouldAnnotate(file: SourceFile): Boolean {
        val coordinates = insertionPoint().locateOccurrence(file.text())
        if (coordinates == nowhere) {
            return false
        }
        if (annotationClass.isRepeatable) {
            return true
        }
        val lineNumber = coordinates.wholeLine
        val lines = file.lines()
        // Subtract 1 because the insertion point refers to the line of
        // a Java type declaration, starting with `class`, `interface`, or `enum`.
        val line = lines[lineNumber - 1]
        val annotationClass = annotationClass.codeReference
        return !line.contains(annotationClass)
    }

    private fun annotationArguments(file: SourceFile): String {
        val args = renderAnnotationArguments(file)
        return if (args.isEmpty()) {
            return ""
        } else {
            "($args)"
        }
    }

    /**
     * Renders the code for passing arguments for the [annotationClass].
     *
     * If there are no arguments to pass, the overriding method must return an empty string.
     */
    protected abstract fun renderAnnotationArguments(file: SourceFile): String

    /**
     * Ensures that the [annotationClass] passed to the constructor satisfies
     * the following criteria:
     *   1. The class is annotated with [@Target][Target].
     *   2. The annotation has [TYPE] as one of the targets.
     *
     * @throws IllegalArgumentException if the criteria are not met.
     */
    internal open fun checkAnnotationClass() {
        val targetClass = Target::class.java
        require(annotationClass.isAnnotationPresent(targetClass)) {
            "The annotation class `${annotationClass.name}`" +
                    " should have `${targetClass.name}`."
        }
        val targets = annotationClass.getAnnotation(targetClass)
        require(targets.value.contains(TYPE)) {
            "Targets of `${annotationClass.name}` do not include ${TYPE.name}."
        }
    }
}

private val <T: Annotation> Class<T>.isRepeatable: Boolean
    get() = isAnnotationPresent(Repeatable::class.java)

internal val <T: Annotation> Class<T>.codeReference: String
    get() = if (isJavaLang) simpleName else name
