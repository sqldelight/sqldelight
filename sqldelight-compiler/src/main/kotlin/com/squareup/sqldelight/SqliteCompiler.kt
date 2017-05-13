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
package com.squareup.sqldelight

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.sqldelight.model.columnName
import com.squareup.sqldelight.model.pathAsType
import com.squareup.sqldelight.model.pathFileName
import com.squareup.sqldelight.resolution.query.QueryResults
import com.squareup.sqldelight.resolution.query.Table
import com.squareup.sqldelight.types.SymbolTable
import java.io.IOException
import java.util.Locale
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class SqliteCompiler {
  private fun write(
      parseContext: SqliteParser.ParseContext,
      status: Status.ValidationStatus.Validated,
      relativePath: String,
      symbolTable: SymbolTable
  ): Status {
    try {
      val className = interfaceName(relativePath.pathFileName())
      val typeSpec = TypeSpec.interfaceBuilder(className)
          .addModifiers(PUBLIC)

      status.queries.filter { it.requiresType }.forEach { queryResults ->
        typeSpec.addType(queryResults.generateInterface())
        typeSpec.addType(queryResults.generateCreator())
        typeSpec.addType(MapperSpec.builder(queryResults).build())
      }

      status.views.forEach { queryResults ->
        val name = queryResults.name

        typeSpec.addField(FieldSpec.builder(String::class.java, "${name.toUpperCase()}_VIEW_NAME")
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("\$S", name)
            .build())

        typeSpec.addType(queryResults.generateInterface())
        typeSpec.addType(queryResults.generateCreator())
      }

      status.queries.filter { it.singleView }
          .map { it.results.first() as QueryResults }
          .distinctBy { it.name }
          .forEach { queryResults ->
            typeSpec.addType(MapperSpec.builder(queryResults).build())
          }

      var table: Table? = null
      if (parseContext.sql_stmt_list().create_table_stmt() != null) {
        table = Table(parseContext.sql_stmt_list().create_table_stmt(), symbolTable)

        typeSpec.addField(FieldSpec.builder(String::class.java, TABLE_NAME)
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("\$S", table.name)
            .build())
            .addType(table.generateCreator())
            .addType(MapperSpec.builder(table).build())

        if (table.javadoc != null) typeSpec.addJavadoc(table.javadoc)

        for (column in table.expand()) {
          if (column.constantName == TABLE_NAME || column.constantName == CREATE_TABLE) {
            throw SqlitePluginException(column.element, "Column name '${column.name}' forbidden")
          }

          val columnConstantBuilder = FieldSpec.builder(String::class.java, column.constantName)
              .addModifiers(PUBLIC, STATIC, FINAL)
              .initializer("\$S", column.name.columnName())

          val methodSpec = MethodSpec.methodBuilder(column.methodName)
              .returns(column.javaType)
              .addModifiers(PUBLIC, ABSTRACT)
          if (!column.javaType.isPrimitive) {
            methodSpec.addAnnotation(if (column.nullable) NULLABLE else NON_NULL)
          }

          if (column.javadocText != null) {
            columnConstantBuilder.addJavadoc(column.javadocText)
            methodSpec.addJavadoc(column.javadocText)
          }

          if (column.annotations != null) {
            methodSpec.addAnnotations(column.annotations)
          }

          typeSpec.addField(columnConstantBuilder.build())
          typeSpec.addMethod(methodSpec.build())
        }
      }
      typeSpec.addType(FactorySpec.builder(table, status, relativePath.pathAsType()).build())

      typeSpec.addTypes(status.sqlStmts.filter { it.needsCompiledStatement }.map { it.programClass() })
      status.sqlStmts.filter { it.needsConstant }.forEach {
        val field = FieldSpec.builder(String::class.java, it.name.toUpperCase())
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("\"\"\n    + \$S", it.sqliteText)
        it.javadoc?.let { field.addJavadoc(it) }
        typeSpec.addField(field.build())
      }
      return Status.Success(parseContext, typeSpec.build())
    } catch (e: SqlitePluginException) {
      return Status.Failure(e.originatingElement, e.message)
    } catch (e: IOException) {
      return Status.Failure(parseContext, e.message ?: "IOException occurred")
    }
  }

  companion object {
    const val TABLE_NAME = "TABLE_NAME"
    const val CREATE_TABLE = "CREATE_TABLE"
    const val FILE_EXTENSION = "sq"
    val OUTPUT_DIRECTORY = listOf("generated", "source", "sqldelight")
    val NULLABLE = ClassName.get("android.support.annotation", "Nullable")
    val NON_NULL = ClassName.get("android.support.annotation", "NonNull")
    val COLUMN_ADAPTER_TYPE = ClassName.get("com.squareup.sqldelight", "ColumnAdapter")

    fun interfaceName(sqliteFileName: String) = sqliteFileName + "Model"
    fun constantName(name: String) = name.toUpperCase(Locale.US)
    fun compile(
        parseContext: SqliteParser.ParseContext,
        status: Status.ValidationStatus.Validated,
        relativePath: String,
        symbolTable: SymbolTable
    ) = SqliteCompiler().write(parseContext, status, relativePath, symbolTable)
  }
}
