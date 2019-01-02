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
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
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

    var propertyPrints = listOf<String>()

    val constructor = FunSpec.constructorBuilder()

    table.columns.forEach { column ->
      val columnName = allocateName(column.columnName)
      typeSpec.addProperty(PropertySpec.builder(columnName, column.type().javaType, OVERRIDE)
          .initializer(columnName)
          .build())
      constructor.addParameter(columnName, column.type().javaType, OVERRIDE)

      propertyPrints += "  $columnName: ${"$"}$columnName"
    }

    typeSpec.addFunction(FunSpec.builder("toString")
        .returns(String::class.asClassName())
        .addModifiers(OVERRIDE)
        .addStatement("return %P", propertyPrints.joinToString(
            separator = "\n",
            prefix = "$typeName.$IMPLEMENTATION_NAME [\n",
            postfix = "\n]")
        )
        .build()
    )

    return typeSpec.primaryConstructor(constructor.build()).build()
  }
}