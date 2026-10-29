/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.compiler.gradle.plugin

import com.google.protobuf.gradle.ProtobufPlugin
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import java.io.File
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("The plugin should exclude the `protoc` output from compilation")
internal class ExcludeProtocOutputSpec {

    /**
     * Reproduces the state in which the `protoc` output directory is among the
     * source directories of the `main` source set, and verifies that the file it
     * contains does not reach the `compileJava` task — see
     * [issue #19](https://github.com/SpineEventEngine/compiler/issues/19).
     *
     * The directory is added after the project is evaluated: the plugin removes it
     * from the source set during the evaluation, so a directory added earlier would
     * not reach the task even without the filter applied to the task itself.
     *
     * The paths are resolved against [Project.getProjectDir][org.gradle.api.Project.getProjectDir],
     * which Gradle canonicalizes, rather than against the given temporary directory,
     * which may be reached via a symbolic link, such as `/var` on macOS. Otherwise,
     * the paths of the same file may differ, and the assertions pass whatever the task
     * source contains.
     */
    @Test
    fun `keeping it out of the Java compile task source`(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.group = "io.spine.stubs"
        with(project) {
            apply(plugin = "java")
            apply<ProtobufPlugin>()
            apply<Plugin>()
            repositories.mavenLocal()
        }
        val projectDir = project.projectDir

        // A file under the default `protoc` output directory.
        val protocJavaDir = projectDir.resolve("build/generated/sources/proto/main/java")
        protocJavaDir.mkdirs()
        val leaked = protocJavaDir.resolve("Leaked.java")
        leaked.writeText("class Leaked {}")

        // A hand-written file, which must reach the task.
        val handWritten = projectDir.resolve("src/main/java/HandWritten.java")
        handWritten.parentFile.mkdirs()
        handWritten.writeText("class HandWritten {}")

        (project as ProjectInternal).evaluate()

        // Re-add the `protoc` output directory after the plugin removed it from
        // the source set during the evaluation, as another plugin may do.
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        sourceSets.getByName("main").java.srcDir(protocJavaDir)

        val compileJava = project.tasks.getByName("compileJava") as JavaCompile
        val sources = compileJava.source.files
        sources shouldContain handWritten
        sources shouldNotContain leaked
    }
}
