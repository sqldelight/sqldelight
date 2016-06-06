/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.model

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.SqliteParser
import javax.lang.model.element.Modifier

internal class Table(
    val interfaceClassName: ClassName,
    val rule: SqliteParser.Create_table_stmtContext,
    val nameAllocator: NameAllocator
) {
  internal val name = rule.table_name().text
  internal val creatorClassName = interfaceClassName.nestedClass("Creator")
  internal val creatorType = ParameterizedTypeName.get(creatorClassName, TypeVariableName.get("T"))
  internal fun column_def() = rule.column_def()
  internal fun column_def(i: Int) = rule.column_def(i)
  internal fun sqliteText() = rule.sqliteText()

  internal fun creatorInterface(): TypeSpec {
    val create = MethodSpec.methodBuilder(Table.CREATOR_METHOD_NAME)
        .returns(TypeVariableName.get("T"))
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)

    for (column in rule.column_def()) {
      create.addParameter(column.javaType, column.methodName(nameAllocator))
    }

    return TypeSpec.interfaceBuilder(Table.CREATOR_CLASS_NAME)
        .addTypeVariable(TypeVariableName.get("T", interfaceClassName))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addMethod(create.build())
        .build()
  }

  companion object {
    val CREATOR_METHOD_NAME = "create"
    val CREATOR_CLASS_NAME = "Creator"
    val CREATOR_FIELD = "creator"
  }
}