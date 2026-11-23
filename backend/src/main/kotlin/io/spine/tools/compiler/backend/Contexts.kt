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

package io.spine.tools.compiler.backend

import io.spine.annotation.VisibleForTesting
import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.base.Identifier.newUuid
import io.spine.core.userId
import io.spine.environment.DefaultMode
import io.spine.server.Application
import io.spine.server.BoundedContext
import io.spine.server.BoundedContext.singleTenant
import io.spine.server.BoundedContextBuilder
import io.spine.server.ServerEnvironment
import io.spine.server.application
import io.spine.server.delivery.Delivery
import io.spine.server.entity.Entity
import io.spine.server.integration.ThirdPartyContext
import io.spine.server.nodeId
import io.spine.server.query.QueryingClient
import io.spine.server.serverEnvironment
import io.spine.server.storage.memory.InMemoryStorageFactory
import io.spine.server.transport.memory.InMemoryTransportFactory
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.plugin.add
import io.spine.tools.compiler.protobuf.ProtoFileList
import io.spine.tools.compiler.type.TypeSystem
import kotlin.reflect.jvm.jvmName

/**
 * The `Code Generation` context is responsible for "hosting" custom views, repositories,
 * and other components of a `BoundedContext` that a user-defined plugin can
 * [add][io.spine.tools.compiler.plugin.Plugin.extend] to a code generation [Pipeline].
 *
 * Views that are available in this context by default are:
 *  * [ProtoSourceFileView] — the view on the source files of the Protobuf model.
 *  * [DependencyView] — the view on the dependencies of the Protobuf model.
 *  * [InsertionPointsView] — the view on the insertion points defined in the current [Pipeline].
 *  * [SettingsView] — the view on the user data configuration.
 *
 * ## Application of the pipeline
 *
 * The context is built in an [Application] of its own, together with the third-party
 * contexts of the pipeline: `Protobuf Compiler`, `Compiler Configuration`, and
 * [`Insertion Points`][insertionPointsContext]. The application has an environment of
 * its own as well, so the contexts of two pipelines never meet, even when the pipelines run
 * in one JVM.
 *
 * [Closing][close] the context closes the application, with all its contexts
 * and its environment.
 */
public class CodeGenerationContext(
    /**
     * The ID of a pipeline that is used to create the context.
     */
    private val pipelineId: String,

    override val typeSystem: TypeSystem,

    /**
     * An optional setup function for the context.
     */
    setup: BoundedContextBuilder.() -> Unit = { }
) : CodegenContext {

    /**
     * Creates a stub instance of the context with empty [TypeSystem].
     *
     * This is a test-only constructor for the cases when resolving of types is unnecessary.
     */
    @VisibleForTesting
    public constructor(pipelineId: String) : this(
        pipelineId,
        TypeSystem(ProtoFileList(emptyList()), emptySet())
    )

    override val insertionPointsContext: ThirdPartyContext =
        ThirdPartyContext.singleTenant("Insertion Points-$pipelineId")

    /**
     * The `Protobuf Compiler` context that emits the events about the compiled Protobuf files.
     */
    internal val protobufCompiler = ProtobufCompilerContext(pipelineId)

    /**
     * The `Compiler Configuration` context that emits the events about the settings
     * of the plugins.
     */
    internal val configuration = ConfigurationContext(pipelineId)

    /**
     * The application that hosts this context and the third-party contexts
     * of the pipeline, and closes them.
     */
    private val application: Application

    /**
     * The underlying instance of the `Code Generation` bounded context.
     */
    public val context: BoundedContext

    init {
        val builder = singleTenant("$NAME_PREFIX$pipelineId").apply {
            add<ProtoSourceFileView>()
            add<DependencyView>()
            add<InsertionPointsView>()
            add<SettingsView>()
        }
        builder.setup()
        val thirdParties = listOf(
            insertionPointsContext,
            protobufCompiler.thirdParty,
            configuration.thirdParty
        )
        application = application {
            serverEnvironment = pipelineEnvironment()
            add(builder)
            thirdParties.forEach { add(it) }
        }
        context = application.contexts().single { it.name() == builder.name() }
    }

    override fun <E : Entity<*, *>> hasEntitiesOfType(cls: Class<E>): Boolean =
        context.hasEntitiesOfType(cls)

    override fun <P : EntityState<*>> select(type: Class<P>): QueryingClient<P> =
        QueryingClient(context, type, this::class.jvmName)

    override fun isOpen(): Boolean = application.isOpen

    override fun close() {
        application.closeIfOpen()
    }

    public companion object {

        /**
         * The prefix for the context names.
         *
         * The prefix is followed by the [pipelineId] in the generated context name.
         */
        @VisibleForTesting
        public const val NAME_PREFIX: String = "Code Generation-"

        /**
         * Creates a new instance of the `Code Generation` bounded context with
         * only the default repositories.
         */
        @JvmStatic
        @JvmOverloads
        @VisibleForTesting
        public fun newInstance(
            pipelineId: String = Pipeline.generateId()
        ): CodeGenerationContext = CodeGenerationContext(pipelineId)
    }
}

/**
 * Creates the environment of the application of a code generation pipeline.
 *
 * In the [default mode][DefaultMode], the contexts keep their data and exchange their
 * messages in memory, and the signals are [delivered directly][Delivery.direct] to their
 * targets, which suits the single-threaded run of a pipeline.
 *
 * In [tests][io.spine.environment.Tests], the environment has the technologies it gives
 * by default to the tests: in-memory storage and transport, and a local `Delivery`.
 */
private fun pipelineEnvironment(): ServerEnvironment = serverEnvironment {
    under<DefaultMode> {
        val storage = InMemoryStorageFactory.newInstance()
        use(storage)
        use(InMemoryTransportFactory.newInstance())
        use(Delivery.direct(nodeId { value = newUuid() }, storage))
    }
}

/**
 * An external bounded context.
 *
 * This context can emit events that are visible to the `Code Generation` context
 * once the [Application] to which the [thirdParty] context is added is built.
 * The application closes the context.
 */
internal sealed class ExternalContext(pipelineId: String, name: String) {

    /**
     * The third-party context that emits the events.
     */
    val thirdParty: ThirdPartyContext = ThirdPartyContext.singleTenant("$name-$pipelineId")

    private val actor = userId { value = name }

    /**
     * Produces and emits events from given event messages.
     */
    fun emitted(events: Sequence<EventMessage>) {
        events.forEach {
            thirdParty.emittedEvent(it, actor)
        }
    }

    /**
     * Produces and emits an event from the given event message.
     */
    fun emitted(singleEvent: EventMessage) =
        emitted(sequenceOf(singleEvent))
}

/**
 * The `Protobuf Compiler` third-party bounded context.
 */
internal class ProtobufCompilerContext(pipelineId: String) :
    ExternalContext(pipelineId, "Protobuf Compiler")

/**
 * The `Compiler Configuration` third-party bounded context.
 */
internal class ConfigurationContext(pipelineId: String) :
    ExternalContext(pipelineId, "Compiler Configuration")
