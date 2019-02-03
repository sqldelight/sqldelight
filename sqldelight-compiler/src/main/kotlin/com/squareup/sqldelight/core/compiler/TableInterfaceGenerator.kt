/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.sqldelight.core.compiler

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import com.squareup.sqldelight.core.lang.ADAPTER_NAME
import com.squareup.sqldelight.core.lang.IMPLEMENTATION_NAME
import com.squareup.sqldelight.core.lang.util.columns
import com.squareup.sqldelight.core.lang.util.sqFile

internal class TableInterfaceGenerator(private val table: SqliteCreateTableStmt) {
  private val typeName = allocateName(table.tableName).capitalize()

  fun kotlinInterfaceSpec(): TypeSpec {
    val typeSpec = TypeSpec.interfaceBuilder(typeName)

    table.columns.forEach { column ->
      typeSpec.addProperty(allocateName(column.columnName), column.type().javaType, PUBLIC)
    }

    val adapters = table.columns.mapNotNull { it.adapter() }

    if (adapters.isNotEmpty()) {
      typeSpec.addType(TypeSpec.classBuilder(ADAPTER_NAME)
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameters(adapters.map {
                ParameterSpec.builder(it.name, it.type, *it.modifiers.toTypedArray()).build()
              })
              .build())
          .addProperties(adapters.map {
            PropertySpec.builder(it.name, it.type, *it.modifiers.toTypedArray())
              .initializer(it.name)
              .build()
          })
          .build())
    }

    return typeSpec
        .addType(kotlinImplementationSpec())
        .build()
  }

  fun kotlinImplementationSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder(IMPLEMENTATION_NAME)
        .addModifiers(DATA)
        .addSuperinterface(ClassName(table.sqFile().packageName, typeName))

    val propertyPrints = CodeBlock.builder()
    propertyPrints.beginControlFlow("buildString")
    propertyPrints.addStatement("appendln(%S)", "$typeName.$IMPLEMENTATION_NAME [")

    val constructor = FunSpec.constructorBuilder()
    val contentToString = MemberName("kotlin.collections", "contentToString")

    table.columns.forEach { column ->
      val columnName = allocateName(column.columnName)
      val columnType = column.type().javaType
      typeSpec.addProperty(PropertySpec.builder(columnName, columnType, OVERRIDE)
          .initializer(columnName)
          .build())
      constructor.addParameter(columnName, columnType, OVERRIDE)

      propertyPrints.addStatement("appendln(%P)", buildCodeBlock {
        add("  $columnName: ")
        if (columnType == ByteArray::class.asTypeName()) {
          add("\${$columnName.%M()}", contentToString)
        } else {
          add("\$$columnName")
        }
      })
    }
    propertyPrints.addStatement("append(%S)", "]")
    propertyPrints.endControlFlow()

    typeSpec.addFunction(FunSpec.builder("toString")
        .returns(String::class.asClassName())
        .addModifiers(OVERRIDE)
        .addCode("return %L", propertyPrints.build())
        .build()
    )

    return typeSpec.primaryConstructor(constructor.build()).build()
  }
}