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
package com.squareup.sqldelight.resolution.query

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.util.javadocText
import org.antlr.v4.runtime.ParserRuleContext
import javax.lang.model.element.Modifier

/**
 * Correponds to a single table result in a SQL select. Example:
 *
 *  1. SELECT table.* FROM table;
 */
internal data class Table private constructor(
    override val javaType: ClassName,
    override val nullable: Boolean,
    internal val table: SqliteParser.Create_table_stmtContext,
    private val nameAllocator: NameAllocator = NameAllocator(),
    override val name: String = table.table_name().text,
    override val element: ParserRuleContext = table.table_name()
) : Result {
  internal val creatorClassName = javaType.nestedClass("Creator")
  internal val creatorType = ParameterizedTypeName.get(creatorClassName, TypeVariableName.get("T"))
  internal val javadoc = javadocText(table.JAVADOC_COMMENT())
  internal val columns by lazy {
    table.column_def().map { Value(it, javaType, name, nameAllocator) }
  }

  constructor(table: SqliteParser.Create_table_stmtContext, symbolTable: SymbolTable) : this (
      symbolTable.tableTypes[table.table_name().text]!!,
      false,
      table
  )

  override fun merge(other: Result) = copy(nullable = nullable || other.nullable)
  override fun tableNames() = listOf(name)
  override fun columnNames() = table.column_def().map { it.column_name().text }
  override fun size() = table.column_def().size
  override fun expand() = columns.map { if (nullable) it.copy(nullable = true) else it }
  override fun findElement(columnName: String, tableName: String?) =
    if (tableName == null || tableName == name) expand().filter { it.name == columnName }
    else emptyList()

  internal fun generateCreator() = TypeSpec.interfaceBuilder(CREATOR_CLASS_NAME)
      .addTypeVariable(TypeVariableName.get("T", javaType))
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addMethod(MethodSpec.methodBuilder(CREATOR_METHOD_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .addParameters(expand().map {
            ParameterSpec.builder(it.javaType, it.methodName).addAnnotations(it.annotations()).build()
          })
          .returns(TypeVariableName.get("T"))
          .build())
      .build()

  companion object {
    const val CREATOR_METHOD_NAME = "create"
    const val CREATOR_CLASS_NAME = "Creator"
    const val CREATOR_FIELD = "creator"
  }
}
