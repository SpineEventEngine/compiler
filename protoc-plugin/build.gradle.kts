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

import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.ToolBase
import io.spine.gradle.publish.StandardJavaPublicationHandler
import org.gradle.api.file.DuplicatesStrategy.INCLUDE

plugins {
    module
}

dependencies {
    implementation(Protobuf.javaLib)
    implementation(Base.lib)?.because("`MessageOrBuilder.toJson()` is needed.")
    implementation(ToolBase.protoCode)?.because("`CodeGeneratorRequestWriter` is needed.")
}

description = "The Spine Compiler plugin for `protoc`."

tasks.jar {
    manifest {
        attributes(mapOf("Main-Class" to "io.spine.tools.compiler.protoc.Plugin"))
    }
    // Assemble the "Fat-JAR" artifact containing all the dependencies.
    // Resolve the classpath lazily: the root project's publishing setup realizes
    // this task, and Gradle forbids resolving this project's configuration there.
    from(configurations.runtimeClasspath.map { classpath ->
        classpath.map {
            when {
                it.isDirectory -> it
                else -> zipTree(it)
            }
        }
    })
    exclude(
        // Protobuf proto source files.
        "google/**",
    )
    // We should provide a classifier or else Protobuf Gradle plugin will
    // substitute it with an OS-specific one.
    archiveClassifier.set("exe")
    duplicatesStrategy = INCLUDE
}

// With the classifier above, the publication has no main artifact, so its POM keeps
// the `pom` packaging Gradle has always derived for it, rather than the `jar` packaging
// which `spinePublishing` declares for standard publications.
// The publication is created on `afterEvaluate` scheduled by the root project,
// so this action, scheduled later, finds it.
afterEvaluate {
    the<PublishingExtension>().publications
        .named<MavenPublication>(StandardJavaPublicationHandler.PUBLICATION_NAME) {
            pom.packaging = "pom"
        }
}
