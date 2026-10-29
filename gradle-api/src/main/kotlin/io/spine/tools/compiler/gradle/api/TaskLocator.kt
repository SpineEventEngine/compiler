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

package io.spine.tools.compiler.gradle.api

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider

/**
 * Abstract base for utilities finding tasks of a certain type by
 * the [provided name pattern][nameFor].
 *
 * The [exists] and [named] functions do not make Gradle create a task registered
 * but not yet created. Prefer them to [find] and [get] during the configuration phase.
 */
public abstract class TaskLocator {

    /**
     * Obtains a name for the task for the given source set.
     */
    public abstract fun nameFor(sourceSet: SourceSet): String

    /**
     * Obtains an instance of the task in the given project for the specified source set.
     *
     * Creates the task if it is registered but not yet created.
     *
     * @see named
     */
    public fun get(project: Project, sourceSet: SourceSet): Task {
        val name = nameFor(sourceSet)
        return project.tasks.getByName(name)
    }

    /**
     * Obtains an instance of the task in the given project for the specified source set.
     *
     * Creates the task if it is registered but not yet created.
     *
     * @return the task or `null` if there is no task created for this source set
     * @see exists
     */
    public fun find(project: Project, sourceSet: SourceSet): Task? {
        val name = nameFor(sourceSet)
        return project.tasks.findByName(name)
    }

    /**
     * Tells if the given project has the task for the specified source set.
     *
     * Unlike [find], does not create the task if it is registered but not yet created.
     */
    public fun exists(project: Project, sourceSet: SourceSet): Boolean =
        nameFor(sourceSet) in project.tasks.names

    /**
     * Obtains a provider of the task in the given project for the specified source set.
     *
     * Unlike [get], does not create the task if it is registered but not yet created.
     *
     * @throws org.gradle.api.UnknownTaskException if there is no task for this source set
     */
    public fun named(project: Project, sourceSet: SourceSet): TaskProvider<Task> =
        project.tasks.named(nameFor(sourceSet))
}
