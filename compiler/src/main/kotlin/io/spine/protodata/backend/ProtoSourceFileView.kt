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

package io.spine.protodata.backend

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.protodata.EnumType
import io.spine.protodata.Field
import io.spine.protodata.FieldName
import io.spine.protodata.FilePath
import io.spine.protodata.MessageType
import io.spine.protodata.OneofGroup
import io.spine.protodata.OneofName
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.Service
import io.spine.protodata.ServiceName
import io.spine.protodata.TypeName
import io.spine.protodata.event.EnumConstantEntered
import io.spine.protodata.event.EnumConstantOptionDiscovered
import io.spine.protodata.event.EnumEntered
import io.spine.protodata.event.EnumOptionDiscovered
import io.spine.protodata.event.FieldEntered
import io.spine.protodata.event.FieldOptionDiscovered
import io.spine.protodata.event.FileEntered
import io.spine.protodata.event.FileOptionDiscovered
import io.spine.protodata.event.OneofGroupEntered
import io.spine.protodata.event.OneofOptionDiscovered
import io.spine.protodata.event.RpcEntered
import io.spine.protodata.event.RpcOptionDiscovered
import io.spine.protodata.event.ServiceEntered
import io.spine.protodata.event.ServiceOptionDiscovered
import io.spine.protodata.event.TypeEntered
import io.spine.protodata.event.TypeOptionDiscovered
import io.spine.protodata.isPartOfOneof
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter

/**
 * A view which collects information about a Protobuf source file.
 */
internal class ProtoSourceFileView
    : View<FilePath, ProtobufSourceFile, ProtobufSourceFile.Builder>() {

    @Subscribe
    fun on(@External e: FileEntered) = alter {
        file = e.file
        header = e.header
    }

    @Subscribe
    fun on(@External e: FileOptionDiscovered) = alter {
        headerBuilder.addOption(e.option)
    }

    @Subscribe
    fun on(@External e: TypeEntered) = alter {
        putType(e.type.typeUrl, e.type)
    }

    @Subscribe
    fun on(@External e: TypeOptionDiscovered) = modifyType(e.type) {
        addOption(e.option)
    }

    @Subscribe
    fun on(@External e: OneofGroupEntered) = modifyType(e.type) {
        addOneofGroup(e.group)
    }

    @Subscribe
    fun on(@External e: OneofOptionDiscovered) = modifyType(e.type) {
        val oneof = findOneof(e.group)
        oneof.addOption(e.option)
    }

    @Subscribe
    fun on(@External e: FieldEntered) = modifyType(e.type) {
        if (e.field.isPartOfOneof) {
            val oneof = findOneof(e.field.oneofName)
            oneof.addField(e.field)
        } else {
            addField(e.field)
        }
    }

    @Subscribe
    fun on(@External e: FieldOptionDiscovered) = modifyType(e.type) {
        val field = findField(e.field)
        field.addOption(e.option)
    }

    @Subscribe
    fun on(@External e: EnumEntered) = alter {
        putEnumType(e.type.typeUrl, e.type)
    }

    @Subscribe
    fun on(@External e: EnumOptionDiscovered) = modifyEnum(e.type) {
        addOption(e.option)
    }

    @Subscribe
    fun on(@External e: EnumConstantEntered) = modifyEnum(e.type) {
        addConstant(e.constant)
    }

    @Subscribe
    fun on(@External e: EnumConstantOptionDiscovered) = modifyEnum(e.type) {
        val const = constantBuilderList.find { it.name == e.constant }!!
        const.addOption(e.option)
    }

    @Subscribe
    fun on(@External e: ServiceEntered) = alter{
        putService(e.service.typeUrl, e.service)
    }

    @Subscribe
    fun on(@External e: ServiceOptionDiscovered) = modifyService(e.service) {
        addOption(e.option)
    }

    @Subscribe
    fun on(@External e: RpcEntered) = modifyService(e.service) {
        addRpc(e.rpc)
    }

    @Subscribe
    fun on(@External e: RpcOptionDiscovered) = modifyService(e.service) {
        rpcBuilderList
            .find { it.name == e.rpc }!!
            .addOption(e.option)
    }

    private fun modifyType(name: TypeName, changes: MessageType.Builder.() -> Unit) {
        val typeUrl = name.typeUrl
        val typeBuilder = builder()
            .getTypeOrThrow(typeUrl)
            .toBuilder()
        changes(typeBuilder)
        builder().putType(typeUrl, typeBuilder.build())
    }

    private fun modifyEnum(name: TypeName, changes: EnumType.Builder.() -> Unit) {
        val typeUrl = name.typeUrl
        val typeBuilder = builder()
            .getEnumTypeOrThrow(typeUrl)
            .toBuilder()
        changes(typeBuilder)
        builder().putEnumType(typeUrl, typeBuilder.build())
    }

    private fun modifyService(name: ServiceName, changes: Service.Builder.() -> Unit) {
        val typeUrl = name.typeUrl
        val builder = builder()
            .getServiceOrThrow(typeUrl)
            .toBuilder()
        changes(builder)
        builder().putService(typeUrl, builder.build())
    }
}

public fun MessageType.Builder.findOneof(name: OneofName): OneofGroup.Builder =
    oneofGroupBuilderList.find { it.name == name }!!

/**
 * Looks up a field by its name in this message type.
 *
 * The field may be found either in the message directly or in one of the `oneof` groups.
 */
public fun MessageType.Builder.findField(name: FieldName): Field.Builder {
    var fieldBuilder = fieldBuilderList.find { it.name == name }
    if (fieldBuilder == null) {
        fieldBuilder = oneofGroupBuilderList
            .flatMap { group -> group.fieldBuilderList }
            .find { it.name == name }
    }
    return fieldBuilder!!
}
