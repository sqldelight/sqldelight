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
package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import app.cash.sqldelight.core.compiler.integration.javadocText
import app.cash.sqldelight.core.lang.ADAPTER_NAME
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin.Companion.isArrayType
import app.cash.sqldelight.core.lang.util.childOfType
import app.cash.sqldelight.core.lang.util.parentOfType
import app.cash.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.psi.util.PsiTreeUtil
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

internal class TableInterfaceGenerator(private val table: LazyQuery) {
  private val typeName = allocateName(table.tableName).capitalize()

  fun kotlinImplementationSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder(typeName)
      .addModifiers(DATA)

    val identifier = PsiTreeUtil.getPrevSiblingOfType(
      PsiTreeUtil.getParentOfType(table.tableName, SqlStmt::class.java),
      SqlDelightStmtIdentifier::class.java
    )
    identifier?.childOfType(SqlTypes.JAVADOC)?.let { javadoc ->
      javadocText(javadoc)?.let { typeSpec.addKdoc(it) }
    }

    val propertyPrints = mutableListOf<CodeBlock>()
    val contentToString = MemberName("kotlin.collections", "contentToString")

    val constructor = FunSpec.constructorBuilder()

    table.columns.forEach { column ->
      val columnName = allocateName(column.columnName)
      val columnType = column.columnType as ColumnTypeMixin
      typeSpec.addProperty(
        PropertySpec.builder(columnName, columnType.type().javaType)
          .initializer(columnName)
          .build()
      )
      val param = ParameterSpec.builder(columnName, columnType.type().javaType)
      column.javadoc?.let(::javadocText)?.let { param.addKdoc(it) }
      constructor.addParameter(param.build())

      propertyPrints += if (columnType.type().javaType.isArrayType) {
        CodeBlock.of("$columnName: \${$columnName.%M()}", contentToString)
      } else {
        CodeBlock.of("$columnName: \$$columnName")
      }
    }

    typeSpec.addFunction(
      FunSpec.builder("toString")
        .returns(String::class.asClassName())
        .addModifiers(OVERRIDE)
        .addStatement(
          "return %L",
          propertyPrints.joinToCode(
            separator = "\n|  ",
            prefix = "\"\"\"\n|$typeName [\n|  ",
            suffix = "\n|]\n\"\"\".trimMargin()"
          )
        )
        .build()
    )

    val adapters = table.columns.mapNotNull { (it.columnType as ColumnTypeMixin).adapter() }

    if (adapters.isNotEmpty()) {
      typeSpec.addType(
        TypeSpec.classBuilder(ADAPTER_NAME)
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameters(
                adapters.map {
                  ParameterSpec.builder(it.name, it.type, *it.modifiers.toTypedArray()).build()
                }
              )
              .build()
          )
          .addProperties(
            adapters.map {
              PropertySpec.builder(it.name, it.type, *it.modifiers.toTypedArray())
                .initializer(it.name)
                .build()
            }
          )
          .build()
      )
    }

    return typeSpec
      .primaryConstructor(constructor.build())
      .build()
  }

  private val LazyQuery.columns: Collection<SqlColumnDef>
    get() = query.columns.map { it.element.parentOfType() }
}
