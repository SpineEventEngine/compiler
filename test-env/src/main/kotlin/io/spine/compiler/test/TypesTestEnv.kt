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

package io.spine.compiler.test

import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_OUTER_CLASSNAME_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_PACKAGE_FIELD_NUMBER
import io.spine.protobuf.pack
import io.spine.compiler.ast.EnumConstant
import io.spine.compiler.ast.EnumType
import io.spine.compiler.ast.Field
import io.spine.compiler.ast.File
import io.spine.compiler.ast.MessageType
import io.spine.compiler.ast.Option
import io.spine.compiler.ast.PrimitiveType.TYPE_BOOL
import io.spine.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.compiler.ast.ProtoFileHeader
import io.spine.compiler.ast.Service
import io.spine.compiler.ast.ServiceName
import io.spine.compiler.ast.TypeName
import io.spine.compiler.ast.constantName
import io.spine.compiler.ast.enumConstant
import io.spine.compiler.ast.fieldName
import io.spine.compiler.ast.fieldType
import io.spine.compiler.ast.type
import io.spine.compiler.ast.file
import io.spine.compiler.ast.messageType
import io.spine.compiler.ast.option
import io.spine.compiler.ast.protoFileHeader
import io.spine.compiler.ast.protobufSourceFile
import io.spine.compiler.ast.service
import io.spine.compiler.ast.serviceName
import io.spine.compiler.ast.toPath
import io.spine.compiler.ast.typeName
import io.spine.compiler.protobuf.ProtoFileList
import io.spine.compiler.type.TypeSystem
import io.spine.compiler.value.pack
import io.spine.compiler.ast.enumType as newEnumType
import io.spine.compiler.ast.field as newField

public object TypesTestEnv {

    public val protoSourceMultiple: File = file { path = "acme/example/multiple.proto" }
    public val protoSourceSingle: File = file { path = "acme/example/single.proto" }
    public val rejectionsFile: File = file {
        path = "acme/example/cartoon_rejections.proto"
    }
    public val multipleFilesOption: Option = option {
        name = "java_multiple_files"
        number = JAVA_MULTIPLE_FILES_FIELD_NUMBER
        type = type { primitive = TYPE_BOOL }
        value = BoolValue.of(true).pack()
    }
    public val javaPackageOption: Option = option {
        name = "java_package"
        number = JAVA_PACKAGE_FIELD_NUMBER
        type = type { primitive = TYPE_STRING }
        value = "dev.acme.example".pack()
    }
    public val outerClassnameOption: Option = option {
        name = "java_outer_classname"
        number = JAVA_OUTER_CLASSNAME_FIELD_NUMBER
        type = type { primitive = TYPE_STRING }
        value = "CartoonRejections".pack()
    }
    public val multipleFilesHeader: ProtoFileHeader = protoFileHeader {
        file = protoSourceMultiple
        packageName = "acme.example"
        option.add(multipleFilesOption)
        option.add(javaPackageOption)
    }
    public val singleFileHeader: ProtoFileHeader = protoFileHeader {
        file = protoSourceSingle
        packageName = "acme.example"
        option.add(javaPackageOption)
    }
    public val rejectionsProtoHeader: ProtoFileHeader = protoFileHeader {
        file = rejectionsFile
        packageName = "acme.example"
        option.add(javaPackageOption)
        option.add(outerClassnameOption)
    }
    public val messageTypeName: TypeName = typeName {
        packageName = multipleFilesHeader.packageName
        simpleName = "Foo"
        typeUrlPrefix = "type.spine.io"
    }
    public val rejectionTypeName: TypeName = typeName {
        packageName = rejectionsProtoHeader.packageName
        simpleName = "CannotDrawCartoon"
        typeUrlPrefix = "type.spine.io"
    }
    private val stringFieldType = fieldType {
        primitive = TYPE_STRING
    }
    public val stringField: Field = newField {
        type = stringFieldType
        name = fieldName { value = "bar" }
        declaringType = messageTypeName
    }
    public val idField: Field = newField {
        type = stringFieldType
        name = fieldName { value = "uuid" }
        declaringType = messageTypeName
    }
    public val messageType: MessageType = messageType {
        file = protoSourceMultiple
        name = messageTypeName
        field.add(stringField)
    }
    public val rejectionType: MessageType = messageType {
        file = rejectionsFile
        name = rejectionTypeName
        field.add(idField)
    }
    public val enumTypeName: TypeName = typeName {
        packageName = multipleFilesHeader.packageName
        typeUrlPrefix = messageTypeName.typeUrlPrefix
        simpleName = "Kind"
    }
    public val undefinedConstant: EnumConstant = enumConstant {
        name = constantName { value = "UNDEFINED" }
        number = 0
        declaredIn = enumTypeName
    }
    public val enumConstant: EnumConstant = enumConstant {
        name = constantName { value = "INSTANCE" }
        number = 1
        declaredIn = enumTypeName
    }
    public val enumType: EnumType = newEnumType {
        file = protoSourceMultiple
        name = enumTypeName
        constant.add(undefinedConstant)
        constant.add(enumConstant)
    }
    public val serviceNameMultiple: ServiceName = serviceName {
        simpleName = "ServiceFromSourceWithMultipleFilesTrue"
        packageName = "multiple.file.sample"
        typeUrlPrefix = "service.spine.io"
    }
    public val serviceNameSingle: ServiceName = serviceName {
        simpleName = "ServiceFromSourceWithMultipleFilesFalse"
        packageName = "single.file.sample"
        typeUrlPrefix = "service.spine.io"
    }
    public val serviceFromMultiple: Service = service {
        file = protoSourceMultiple
        name = serviceNameMultiple
    }
    public val serviceFromSingle: Service = service {
        file = protoSourceSingle
        name = serviceNameSingle
    }
    public val typeSystem: TypeSystem = run {
        val multipleFilesProto = protobufSourceFile {
            file = protoSourceMultiple
            header = multipleFilesHeader
            type.put(messageTypeName.typeUrl, messageType)
            enumType.put(enumTypeName.typeUrl, TypesTestEnv.enumType)
            service.put(serviceNameMultiple.typeUrl, serviceFromMultiple)
        }
        val singleFileProto = protobufSourceFile {
            file = protoSourceSingle
            header = singleFileHeader
            service.put(serviceNameSingle.typeUrl, serviceFromSingle)
        }
        val rejections = protobufSourceFile {
            file = rejectionsFile
            header = rejectionsProtoHeader
            type.put(rejectionTypeName.typeUrl, rejectionType)
        }
        TypeSystem(
            ProtoFileList(listOf(
                protoSourceMultiple.toPath().toFile(),
                protoSourceSingle.toPath().toFile(),
                rejectionsFile.toPath().toFile()
            )),
            setOf(
                multipleFilesProto,
                singleFileProto,
                rejections
        ))
    }
}
