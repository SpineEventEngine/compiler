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

@file:Suppress(
    "TooManyFunctions", // Deprecated functions will be kept for a while.
    "ConstPropertyName"
)

package io.spine.gradle

import io.spine.gradle.publish.PublishingRepos
import java.io.File
import java.net.URI
import java.util.*
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.maven

/**
 * Registers the standard set of Maven repositories.
 *
 * To be used in `buildscript` clauses when a fully-qualified call must be made.
 */
@Suppress("unused")
@Deprecated(
    message = "Please use `standardSpineSdkRepositories()`.",
    replaceWith = ReplaceWith("standardSpineSdkRepositories()")
)
fun doApplyStandard(repositories: RepositoryHandler) = repositories.standardToSpineSdk()

/**
 * A scrambled version of PAT generated with the only "read:packages" scope.
 *
 * The scrambling around PAT is necessary because GitHub analyzes commits for the presence
 * of tokens and invalidates them.
 *
 * @see <a href="https://github.com/orgs/community/discussions/25629">
 *     How to make GitHub packages to the public</a>
 */
object Pat {
    private const val shade = "_phg->8YlN->MFRA->gxIk->HVkm->eO6g->FqHJ->z8MS->H4zC->ZEPq"
    private const val separator = "->"
    private val chunks: Int = shade.split(separator).size - 1

    fun credentials(): Credentials {
        val pass = shade.replace(separator, "").splitAndReverse(chunks, "")
        return Credentials("public", pass)
    }

    /**
     * Splits this string to the chunks, reverses each chunk, and joins them
     * back to a string using the [separator].
     */
    private fun String.splitAndReverse(numChunks: Int, separator: String): String {
        check(length / numChunks >= 2) {
            "The number of chunks is too big. Must be <= ${length / 2}."
        }
        val chunks = chunked(length / numChunks)
        val reversedChunks = chunks.map { chunk -> chunk.reversed() }
        return reversedChunks.joinToString(separator)
    }
}

/**
 * Adds a read-only view to all artifacts of the SpineEventEngine
 * GitHub organization.
 */
fun RepositoryHandler.spineArtifacts(): MavenArtifactRepository = maven {
    url = URI("https://maven.pkg.github.com/SpineEventEngine/*")
    includeSpineOnly()
    val pat = Pat.credentials()
    credentials {
        username = pat.username
        password = pat.password
    }
}

val RepositoryHandler.intellijReleases: MavenArtifactRepository
    get() = maven("https://www.jetbrains.com/intellij-repository/releases")

val RepositoryHandler.jetBrainsCacheRedirector: MavenArtifactRepository
    get() = maven("https://cache-redirector.jetbrains.com/intellij-dependencies")

/**
 * Applies repositories commonly used by Spine Event Engine projects.
 */
fun RepositoryHandler.standardToSpineSdk() {
    spineArtifacts()

    val spineRepos = listOf(
        Repos.artifactRegistry,
        Repos.artifactRegistrySnapshots
    )

    spineRepos
        .map { URI(it) }
        .forEach {
            maven {
                url = it
                includeSpineOnly()
            }
        }

    intellijReleases
    jetBrainsCacheRedirector

    maven {
        url = URI(Repos.sonatypeSnapshots)
    }

    mavenCentral()
    gradlePluginPortal()
    mavenLocal().includeSpineOnly()
}

@Deprecated(
    message = "Please use `standardToSpineSdk() instead.",
    replaceWith = ReplaceWith("standardToSpineSdk()")
)
fun RepositoryHandler.applyStandard() = this.standardToSpineSdk()

/**
 * A Maven repository.
 */
data class Repository(
    val releases: String,
    val snapshots: String,
    private val credentialsFile: String? = null,
    private val credentialValues: ((Project) -> Credentials?)? = null,
    val name: String = "Maven repository `$releases`"
) {

    /**
     * Obtains the publishing password credentials to this repository.
     *
     * If the credentials are represented by a `.properties` file, reads the file and parses
     * the credentials. The file must have properties `user.name` and `user.password`, which store
     * the username and the password for the Maven repository auth.
     */
    fun credentials(project: Project): Credentials? = when {
        credentialValues != null -> credentialValues.invoke(project)
        credentialsFile != null -> credsFromFile(credentialsFile, project)
        else -> throw IllegalArgumentException(
            "Credentials file or a supplier function should be passed."
        )
    }

    private fun credsFromFile(fileName: String, project: Project): Credentials? {
        val file = project.rootProject.file(fileName)
        if (file.exists().not()) {
            return null
        }

        val log = project.logger
        log.info("Using credentials from `$fileName`.")
        val creds = file.parseCredentials()
        log.info("Publishing build as `${creds.username}`.")
        return creds
    }

    private fun File.parseCredentials(): Credentials {
        val properties = Properties().apply { load(inputStream()) }
        val username = properties.getProperty("user.name")
        val password = properties.getProperty("user.password")
        return Credentials(username, password)
    }

    override fun toString(): String {
        return name
    }
}

/**
 * Password credentials for a Maven repository.
 */
data class Credentials(
    val username: String?,
    val password: String?
)

/**
 * Defines names of additional repositories commonly used in the Spine SDK projects.
 *
 * @see [applyStandard]
 */
private object Repos {
    val artifactRegistry = PublishingRepos.cloudArtifactRegistry.releases
    val artifactRegistrySnapshots = PublishingRepos.cloudArtifactRegistry.snapshots
    const val sonatypeSnapshots = "https://oss.sonatype.org/content/repositories/snapshots"
}

/**
 * Narrows down the search for this repository to Spine-related artifact groups.
 */
private fun MavenArtifactRepository.includeSpineOnly() {
    content {
        includeGroupByRegex("io\\.spine.*")
    }
}
