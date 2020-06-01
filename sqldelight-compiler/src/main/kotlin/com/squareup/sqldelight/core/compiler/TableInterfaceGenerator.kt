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

import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import com.squareup.sqldelight.core.lang.ADAPTER_NAME
import com.squareup.sqldelight.core.lang.psi.ColumnDefMixin
import com.squareup.sqldelight.core.lang.psi.ColumnDefMixin.Companion.isArrayType
import com.squareup.sqldelight.core.lang.util.parentOfType

internal class TableInterfaceGenerator(private val table: LazyQuery) {
  private val typeName = allocateName(table.tableName).capitalize()

  fun kotlinImplementationSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder(typeName)
        .addModifiers(DATA)

    val propertyPrints = mutableListOf<CodeBlock>()
    val contentToString = MemberName("kotlin.collections", "contentToString")

    val constructor = FunSpec.constructorBuilder()

    table.columns.forEach { column ->
      val columnName = allocateName(column.columnName)
      typeSpec.addProperty(PropertySpec.builder(columnName, column.type().javaType)
          .initializer(columnName)
          .build())
      constructor.addParameter(columnName, column.type().javaType)

      propertyPrints += if (column.type().javaType.isArrayType) {
        CodeBlock.of("$columnName: \${$columnName.%M()}", contentToString)
      } else {
        CodeBlock.of("$columnName: \$$columnName")
      }
    }

    typeSpec.addFunction(FunSpec.builder("toString")
        .returns(String::class.asClassName())
        .addModifiers(OVERRIDE)
        .addStatement("return %L", propertyPrints.joinToCode(
            separator = "\n|  ",
            prefix = "\"\"\"\n|$typeName [\n|  ",
            suffix = "\n|]\n\"\"\".trimMargin()")
        )
        .build()
    )

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
        .primaryConstructor(constructor.build())
        .build()
  }

  private val LazyQuery.columns: Collection<ColumnDefMixin>
    get() = query.columns.map { it.element.parentOfType<ColumnDefMixin>() }
}
