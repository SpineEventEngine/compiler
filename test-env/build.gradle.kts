/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import io.spine.dependency.lib.AutoService
import io.spine.dependency.lib.Grpc

plugins {
    module
    prototap
    `build-proto-model`
}

dependencies {
    annotationProcessor(AutoService.processor)
    compileOnly(AutoService.annotations)

    implementation(project(":backend"))
    implementation(platform(Grpc.bom))
    implementation(Grpc.stub)
    implementation(Grpc.protobuf)
}

val grpcPluginName = "grpc"

protobuf {
    generateProtoTasks {
        all().whenTaskAdded {
            plugins {
                maybeCreate(grpcPluginName)
            }
        }
    }
}

prototap {
    sourceSet.set(sourceSets.main.get())
}

// Add resources placed by ProtoTap so that we can use them from tests in other modules.
tasks.jar {
    from(tasks.processTestResources)
}

/**
 * We only need to publish `test-env` locally for integration tests.
 * Do not publish to public Maven repositories.
 * 
 * See https://bit.ly/gradle-cond-pub for details.
 */
tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf { false }
}

/**
 * No need to generate the documentation for test environment code.
 */
disableDocumentationTasks()
