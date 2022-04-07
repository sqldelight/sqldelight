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
import app.cash.sqldelight.core.lang.util.childOfType
import app.cash.sqldelight.core.lang.util.columnDefSource
import app.cash.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

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

    val constructor = FunSpec.constructorBuilder()

    table.query.columns.map { it.element as NamedElement }.forEach { column ->
      val columnName = allocateName(column)
      val columnDef = column.columnDefSource()!!
      val columnType = columnDef.columnType as ColumnTypeMixin
      val javaType = columnType.type().javaType
      val typeWithoutAnnotations = javaType.copy(annotations = emptyList())
      typeSpec.addProperty(
        PropertySpec.builder(columnName, typeWithoutAnnotations)
          .initializer(columnName)
          .addAnnotations(javaType.annotations)
          .build()
      )
      val param = ParameterSpec.builder(columnName, typeWithoutAnnotations)
      columnDef.javadoc?.let(::javadocText)?.let { param.addKdoc(it) }
      constructor.addParameter(param.build())
    }

    val adapters = table.query.columns
      .map { (it.element as NamedElement).columnDefSource()!! }
      .mapNotNull { (it.columnType as ColumnTypeMixin).adapter() }

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
}
