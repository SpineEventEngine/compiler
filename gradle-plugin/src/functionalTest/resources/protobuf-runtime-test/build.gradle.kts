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

import io.spine.gradle.repo.standardToSpineSdk
import io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler

buildscript {
    standardSpineSdkRepositories()
}

group = "io.spine.tools.test"
version = "1.0.0-SNAPSHOT"

plugins {
    java
    kotlin("jvm")
    id("com.google.protobuf")
    id("@COMPILER_PLUGIN_ID@") version "@COMPILER_VERSION@"
}

repositories {
    mavenLocal()
    standardToSpineSdk()
}

// Unlike Spine builds, this project does not force `Protobuf.libs`, so the Protobuf
// runtime of the Compiler JVM comes from dependency resolution alone.
// Only `Base.lib` is forced, as in the other fixtures.
configurations.all {
    resolutionStrategy {
        force(
            io.spine.dependency.local.Base.lib,
        )
    }
}

dependencies {
    spineCompiler(project(":plugin"))
}

spine {
    compiler {
        plugins("io.spine.tools.compiler.test.GencodePlugin")
    }
}

tasks.withType<LaunchSpineCompiler>().configureEach {
    // The functional tests turn off the Protobuf gencode/runtime version check
    // for the whole build. Turn it back on in the Compiler JVM, where the gencode
    // of the plugin must not find a runtime older than itself.
    environment.remove("TEMPORARILY_DISABLE_PROTOBUF_VERSION_CHECK")
    // Record the classpath of the Compiler JVM, letting the functional test
    // check which Protobuf runtime comes first.
    val classpathFile = layout.projectDirectory.file("compiler-classpath.txt")
    doLast {
        classpathFile.asFile.writeText(classpath.asPath)
    }
}
