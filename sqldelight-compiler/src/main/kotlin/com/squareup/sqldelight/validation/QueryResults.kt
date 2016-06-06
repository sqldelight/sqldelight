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
package com.squareup.sqldelight.validation

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.MapperSpec
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.model.Type
import com.squareup.sqldelight.model.isHandledType
import com.squareup.sqldelight.model.javaType
import com.squareup.sqldelight.model.parentTable
import com.squareup.sqldelight.model.pathAsType
import com.squareup.sqldelight.resolution.Resolution
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.types.Value
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import javax.lang.model.element.Modifier

data class QueryResults private constructor(
    val relativePath: String,
    internal val queryName: String,
    /**
     * Individual columns that map to a single SQLite data type. Each one will have a method in the
     * generated interface.
     */
    internal val columns: Map<String, IndexedValue>,
    /**
     * Full tables that are part of the query results. Each one will have a method in
     * the generated interface, and creating a mapper for this query will require its own
     * RowMapper<TableType>.
     */
    internal val tables: Map<String, QueryTable>
) {
  private val modelInterface = relativePath.pathAsType()
  internal val interfaceClassName = "${queryName.capitalize()}Model"
  internal val interfaceType = modelInterface.nestedClass("${queryName.capitalize()}Model")
  internal val creatorType = modelInterface.nestedClass("${queryName.capitalize()}Creator")
  internal val requiresType = columns.size + tables.size > 1
  internal val mapperName = "${queryName.capitalize()}${MapperSpec.MAPPER_NAME}"

  internal fun <T> sortedResultsMap(
      columnsMap: (String, IndexedValue) -> T,
      tablesMap: (String, QueryTable) -> T
  ) = columns.map { it.value.index to columnsMap(it.key, it.value) }
      .plus(tables.map { it.value.index to tablesMap(it.key, it.value) })
      .sortedBy { it.first }
      .map { it.second }

  internal fun generateInterface() = TypeSpec.interfaceBuilder(interfaceClassName)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addMethods(sortedResultsMap(
          { columnName, value -> value.interfaceMethod(columnName) },
          { tableName, table -> table.interfaceMethod(tableName) }
      ))
      .build()

  internal fun generateCreator() = TypeSpec.interfaceBuilder("${queryName.capitalize()}Creator")
      .addTypeVariable(TypeVariableName.get("T", interfaceType))
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addMethod(MethodSpec.methodBuilder("create")
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .addParameters(sortedResultsMap(
              { columnName, value -> value.parameterSpec(columnName) },
              { tableName, table -> table.parameterSpec(tableName) }
          ))
          .returns(TypeVariableName.get("T"))
          .build())
      .build()

  internal fun foreignTypes() = columns.values
      .filter { !(it.value.columnDef?.isHandledType ?: true) }
      .map { it.index to it.tableInterface }
      .plus(tables.map { it.value.index to it.value.interfaceType })
      .sortedBy { it.first }
      .map { it.second }
      .filterNotNull()
      .distinct()

  companion object {
    /**
     * Take as input the resolution of a select statement and the current context's SymbolTable
     * and return a query results object consumable by the compiler.
     */
    internal fun create(
        relativePath: String,
        resolution: Resolution,
        symbolTable: SymbolTable,
        queryName: String
    ): QueryResults {
      val methodNames = LinkedHashSet<String>()

      // First separate out all the columns with tables from the expressions.
      val columnsForTableName = LinkedHashMap<String, MutableSet<IndexedValue>>()
      val columnDefsForTableName = LinkedHashMap<String, MutableSet<SqliteParser.Column_defContext>>()
      val expressions = ArrayList<IndexedValue>()
      resolution.values.forEachIndexed { index, value ->
        val indexedValue = IndexedValue(index, value,
            symbolTable.tableTypes[value.columnDef?.parentTable()?.table_name()?.text])

        if (value.tableName != null && value.element is SqliteParser.Column_defContext) {
          // Value is part of a table.
          val columnDefs = columnDefsForTableName.getOrPut(value.tableName) {
            LinkedHashSet<SqliteParser.Column_defContext>()
          }

          if (!columnDefs.add(value.element)) {
            // This column was already added so we have a duplicate.
            expressions.add(indexedValue)
          } else {
            // Column not yet added
            val columns = columnsForTableName.getOrPut(value.tableName) {
              LinkedHashSet<IndexedValue>()
            }
            columns.add(indexedValue)
          }
        } else {
          // Value is an expression or was aliased.
          expressions.add(indexedValue)
        }
      }

      // Find the complete tables in the resolution.
      val tables = LinkedHashMap<String, QueryTable>()
      symbolTable.tables.forEach { mapEntry ->
        val originalTableName = mapEntry.key
        val createTable = mapEntry.value
        columnsForTableName.forEach { tableName, columns ->
          if (createTable.column_def().size == columns.size &&
              createTable.column_def().containsAll(columns.map { it.value.element })) {
            // Same table, add it as a full table query result.
            methodNames.add(tableName)
            tables.put(tableName, QueryTable(createTable, columns,
                symbolTable.tableTypes[originalTableName]!!))
          }
        }
      }

      // Take the columns from incomplete tables in the resolution and add them to the expression list.
      expressions.addAll(columnsForTableName
          .filter { !tables.keys.contains(it.key) }
          .flatMap { it.value })

      // Add all the expressions in as individual columns.
      val columns = LinkedHashMap<String, IndexedValue>()
      for((index, value) in expressions) {
        val indexedValue = IndexedValue(index, value,
            symbolTable.tableTypes[value.columnDef?.parentTable()?.table_name()?.text])
        var methodName: String
        if (value.columnName != null) {
          methodName = value.columnName
          if (methodNames.add(methodName)) {
            columns.put(methodName, indexedValue)
            continue
          }

          if (value.tableName != null) {
            methodName = "${value.tableName}_${value.columnName}"
            if (methodNames.add(methodName)) {
              columns.put(methodName, indexedValue)
              continue
            }
          }
        } else if (value.element is SqliteParser.ExprContext) {
          methodName = value.element.methodName() ?: "expr"
        } else if (value.element is SqliteParser.Literal_valueContext) {
          methodName = value.element.methodName()
        } else {
          methodName = value.element.text
        }

        var i = 2
        var suffixedMethodName = methodName
        while (!methodNames.add(suffixedMethodName)) {
          suffixedMethodName = "${methodName}_${i++}"
        }
        columns.put(suffixedMethodName, indexedValue)
      }

      return QueryResults(relativePath, queryName, columns, tables)
    }

    private fun SqliteParser.ExprContext.methodName(): String? {
      if (column_name() != null) {
        if (table_name() != null) {
          return "${table_name().text}_${column_name().text}"
        }
        return column_name().text
      }
      if (literal_value() != null) {
        return literal_value().methodName()
      }
      if (function_name() != null) {
        if (expr().size == 0) {
          return function_name().text
        }
        return "${function_name().text}_${expr(0).methodName() ?: return function_name().text}"
      }
      return null
    }

    private fun SqliteParser.Literal_valueContext.methodName(): String {
      if (INTEGER_LITERAL() != null) {
        return "int_literal"
      }
      if (REAL_LITERAL() != null) {
        return "real_literal"
      }
      if (STRING_LITERAL() != null) {
        return "string_literal"
      }
      if (BLOB_LITERAL() != null) {
        return "blob_literal"
      }
      return "literal"
    }
  }

  /**
   * Keep track of the index for values so that we can avoid using Cursor.getColumnIndex()
   */
  internal data class IndexedValue(
      val index: Int,
      val value: Value,
      val tableInterface: ClassName?
  ) {
    internal val javaType = value.columnDef?.javaType ?: when (value.type) {
      Value.SqliteType.INTEGER -> Type.INTEGER.defaultType
      Value.SqliteType.REAL -> Type.REAL.defaultType
      Value.SqliteType.BLOB -> Type.BLOB.defaultType
      Value.SqliteType.TEXT -> Type.TEXT.defaultType
      Value.SqliteType.NULL -> TypeName.VOID
    }

    fun parameterSpec(parameterName: String) = ParameterSpec.builder(javaType, parameterName)
        .build()

    fun interfaceMethod(methodName: String) = MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .returns(javaType)
        .build()
  }

  /**
   * The compiler expects an ANTLR rule so keep track of both it and the indexed values.
   */
  internal data class QueryTable(
      val table: SqliteParser.Create_table_stmtContext,
      val indexedValues: Set<IndexedValue>,
      val interfaceType: ClassName
  ) {
    val index = indexedValues.first().index

    fun parameterSpec(parameterName: String) = ParameterSpec.builder(interfaceType, parameterName)
        .build()

    fun interfaceMethod(methodName: String) = MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .returns(interfaceType)
        .build()
  }
}
