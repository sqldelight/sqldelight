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
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.FINAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.lang.util.columns
import com.squareup.sqldelight.core.lang.util.sqFile

internal class TableInterfaceGenerator(private val table: SqliteCreateTableStmt) {
  fun interfaceSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder("${table.tableName.name.capitalize()}Model")
        .addModifiers(ABSTRACT)
        .addSuperinterface(ClassName(table.sqFile().packageName, table.tableName.name.capitalize()))

    table.columns.forEach { column ->
      typeSpec.addFunction(FunSpec.builder(column.columnName.name)
          .addModifiers(PUBLIC, ABSTRACT)
          .returns(column.type())
          .build())

      typeSpec.addProperty(PropertySpec.builder(column.columnName.name, column.type(), OVERRIDE, FINAL)
          .getter(FunSpec.getterBuilder().addStatement("return ${column.columnName.name}()").build())
          .build())
    }


    return typeSpec.build()
  }

  fun kotlinInterfaceSpec(): TypeSpec {
    val typeSpec = TypeSpec.interfaceBuilder(table.tableName.name.capitalize())

    table.columns.forEach { column ->
      typeSpec.addProperty(column.columnName.name, column.type(), PUBLIC)
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
    val typeSpec = TypeSpec.classBuilder("Impl")
        .addModifiers(DATA)
        .addSuperinterface(ClassName(table.sqFile().packageName, table.tableName.name.capitalize()))

    val constructor = FunSpec.constructorBuilder()

    table.columns.forEach { column ->
      typeSpec.addProperty(PropertySpec.builder(column.columnName.name, column.type(), OVERRIDE)
          .initializer(column.columnName.name)
          .build())
      constructor.addParameter(column.columnName.name, column.type(), OVERRIDE)
    }

    return typeSpec.primaryConstructor(constructor.build()).build()
  }

  companion object {
    internal const val ADAPTER_NAME = "Adapter"
  }
}