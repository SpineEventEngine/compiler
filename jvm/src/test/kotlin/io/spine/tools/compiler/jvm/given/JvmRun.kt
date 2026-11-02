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

package io.spine.tools.compiler.jvm.given

import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit.MINUTES
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.io.path.Path
import kotlin.io.path.outputStream
import kotlin.io.path.readText

/**
 * The outcome of a program run in a separate JVM.
 *
 * @property exitCode The exit code of the JVM process.
 * @property output The combined standard output and error of the process.
 */
internal data class JvmRun(val exitCode: Int, val output: String)

/**
 * Runs the `main` method of the given class in a new JVM with the classpath of this JVM.
 *
 * The new JVM has no classes initialized by the code of this JVM, which lets a test
 * control the order of class initialization.
 *
 * The classpath is passed via a manifest-only JAR so that the command line stays
 * within the length limits of all platforms.
 *
 * @param mainClass The class declaring the static `main` method to run.
 * @param workDir The directory for the classpath JAR.
 */
internal fun runInFreshJvm(mainClass: Class<*>, workDir: Path): JvmRun {
    val java = Path(System.getProperty("java.home"), "bin", "java")
    val classpathJar = writeClasspathJar(workDir)
    val outputFile = workDir.resolve("output.txt")
    val process = ProcessBuilder(java.toString(), "-cp", classpathJar.toString(), mainClass.name)
        .redirectErrorStream(true)
        .redirectOutput(outputFile.toFile())
        .start()
    if (!process.waitFor(1, MINUTES)) {
        process.destroyForcibly()
        error("The JVM running `${mainClass.name}` did not exit in time.")
    }
    return JvmRun(process.exitValue(), outputFile.readText())
}

private fun writeClasspathJar(workDir: Path): Path {
    val entries = System.getProperty("java.class.path")
        .split(File.pathSeparator)
        .joinToString(" ") { File(it).toURI().toString() }
    val manifest = Manifest().apply {
        mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        mainAttributes[Attributes.Name.CLASS_PATH] = entries
    }
    val jar = workDir.resolve("classpath.jar")
    JarOutputStream(jar.outputStream(), manifest).use { }
    return jar
}
