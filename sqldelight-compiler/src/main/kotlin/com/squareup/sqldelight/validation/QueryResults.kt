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
import com.squareup.javapoet.ParameterizedTypeName
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
import com.squareup.sqldelight.resolution.Resolver
import com.squareup.sqldelight.resolution.resolve
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.types.Value
import org.antlr.v4.runtime.ParserRuleContext
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import javax.lang.model.element.Modifier

data class QueryResults private constructor(
    val relativePath: String,
    internal val isView: Boolean,
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
    internal val tables: Map<String, QueryTable>,
    /**
     * Views are other queries that are recursively added here.
     */
    internal val views: Map<String, QueryResults>,
    /**
     * Index of the first value in this query. Queries can be recursive with views.
     */
    internal val index: Int,
    private val modelInterface: ClassName
) {
  internal val types: Map<TypeName, TypeVariableName>
  internal val interfaceClassName = "${queryName.capitalize()}Model"
  internal val interfaceType = modelInterface.nestedClass(interfaceClassName)
  internal val creatorType = modelInterface.nestedClass("${queryName.capitalize()}Creator")
  internal val mapperName = "${queryName.capitalize()}${MapperSpec.MAPPER_NAME}"
  internal val mapperType = modelInterface.nestedClass(mapperName)
  internal val requiresType = columns.size + tables.size + views.size > 1 || isView
  internal val singleView = tables.isEmpty() && columns.isEmpty() && views.size == 1

  init {
    // Initialize the types map.
    val types = LinkedHashMap<TypeName, TypeVariableName>()
    tables.values.forEach {
      types.putIfAbsent(it.interfaceType, TypeVariableName.get("T${it.index+1}", it.interfaceType))
    }
    views.values.forEach { view ->
      // For each type we are adding to satisfy the view, we have to re-do its bounds to
      // whatever this QueryResult has already generated types for.
      view.types.forEach {
        val bound = it.value.bounds.first()
        val newBound: TypeName
        if (bound is ClassName) {
          // Table or parameterless view - add the type to our map as if it were our own table.
          newBound = bound
        } else if (bound is ParameterizedTypeName) {
          // View - check the type arguments, which are guaranteed TypeVariableNames (see below
          // where we add the view itself) and use the TypeVariableName found in our own map
          // instead.
          newBound = ParameterizedTypeName.get(
              bound.rawType,
              *bound.typeArguments.map { types[(it as TypeVariableName).bounds.first()] }.toTypedArray()
          )
        } else {
          throw IllegalStateException("Unexpected type variable $bound")
        }
        types.putIfAbsent(it.key, TypeVariableName.get("V${view.index+1}${it.value.name}", newBound))
      }
      // Add the type for the view itself.
      types.putIfAbsent(view.interfaceType, TypeVariableName.get("V${view.index+1}", view.queryBound(types)))
    }
    this.types = types
  }

  /**
   * For the given QueryResults, form a TypeVariable that has bounds corresponding
   * to the current instance's type map. However if the QueryResults passed in has no types
   * associated with it, the bound is just the type itself (it is not Parameterized).
   */
  internal fun queryBound(types: Map<TypeName, TypeVariableName> = this.types) =
    if (this.types.isEmpty()) {
      interfaceType
    } else {
      ParameterizedTypeName.get(
          interfaceType,
          *this.types.keys.map { types[it] }.toTypedArray()
      )
    }

  internal fun isEmpty() = columns.isEmpty() && tables.isEmpty() && views.isEmpty()

  internal fun <T> sortedResultsMap(
      columnsMap: (String, IndexedValue) -> T,
      tablesMap: (String, QueryTable) -> T,
      viewsMap: (String, QueryResults) -> T
  ) = columns.map { it.value.index to columnsMap(it.key, it.value) }
      .plus(tables.map { it.value.index to tablesMap(it.key, it.value) })
      .plus(views.map { it.value.index to viewsMap(it.key, it.value) })
      .sortedBy { it.first }
      .map { it.second }

  internal fun generateInterface() = TypeSpec.interfaceBuilder(interfaceClassName)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addTypeVariables(types.values)
      .addMethods(sortedResultsMap(
          { columnName, value -> value.interfaceMethod(columnName) },
          { tableName, table -> MethodSpec.methodBuilder(tableName)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .returns(types[table.interfaceType])
              .build() },
          { viewName, view -> MethodSpec.methodBuilder(viewName)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .returns(types[view.interfaceType])
              .build()
          }
      ))
      .build()

  internal fun generateCreator() = TypeSpec.interfaceBuilder("${queryName.capitalize()}Creator")
      .addTypeVariables(types.values + TypeVariableName.get("T", queryBound()))
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addMethod(MethodSpec.methodBuilder("create")
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .addParameters(sortedResultsMap(
              { columnName, value -> value.parameterSpec(columnName) },
              { tableName, table -> ParameterSpec.builder(types[table.interfaceType], tableName).build() },
              { viewName, view -> ParameterSpec.builder(types[view.interfaceType], viewName).build() }
          ))
          .returns(TypeVariableName.get("T"))
          .build())
      .build()

  internal fun foreignTypes() = columns.values
      .filter { !(it.value.columnDef?.isHandledType ?: true) }
      .map { it.index to it.tableInterface }
      .plus(tables.map { it.value.index to it.value.interfaceType })
      .sortedBy { it.first }
      .filter { it.second != null }
      .map { it.first to it.second!! }
      .distinctBy { it.second }

  companion object {
    /**
     * Take as input the resolution of a select statement and the current context's SymbolTable
     * and return a query results object consumable by the compiler.
     */
    internal fun create(
        relativePath: String,
        resolution: Resolution,
        symbolTable: SymbolTable,
        queryName: String,
        isView: Boolean = false,
        tableIndex: Int = 0,
        modelInterface: ClassName = relativePath.pathAsType()
    ): QueryResults {
      val methodNames = LinkedHashSet<String>()

      // First separate out all the columns with tables from the expressions.
      val columnsForTableName = LinkedHashMap<String, MutableSet<IndexedValue>>()
      val originalTableNames = LinkedHashMap<String, String>() // Aliases to original table names.
      val expressions = ArrayList<IndexedValue>()
      resolution.values.forEachIndexed { index, value ->
        val tableName = value.columnDef?.parentTable()?.table_name()?.text
        val indexedValue = IndexedValue(tableIndex + index, value,
            symbolTable.tableTypes[tableName], tableName)

        if (value.tableName != null && value.originalTableName != null) {
          // Column not yet added
          val columns = columnsForTableName.getOrPut(value.tableName) {
            LinkedHashSet<IndexedValue>()
          }
          originalTableNames.put(value.tableName, value.originalTableName)
          columns.add(indexedValue)
        } else {
          // Value is an expression or was aliased.
          expressions.add(indexedValue)
        }
      }

      // Find the complete tables in the resolution.
      val tables = LinkedHashMap<String, QueryTable>()
      symbolTable.tables.matchTableName(columnsForTableName, originalTableNames) {
        originalTableName, createTable, tableName, columns ->

        if (columns.map { it.value.element }.containsAll(createTable.column_def())) {
          // Same table, add it as a full table query result.
          val (indexedViewColumns, leftovers) = tableColumns(columns,
              createTable.column_def().map { Value(createTable.table_name(), it) })
          methodNames.add(tableName)
          tables.put(tableName, QueryTable(createTable, indexedViewColumns,
              symbolTable.tableTypes[originalTableName]!!))
          expressions.addAll(leftovers)
        }
      }

      val views = LinkedHashMap<String, QueryResults>()
      symbolTable.views.matchTableName(columnsForTableName, originalTableNames) {
        originalViewName, createView, viewName, columns ->

        val viewColumns = Resolver(symbolTable).resolve(createView.select_stmt())
        if (viewColumns.errors.isEmpty() && columns.map { it.value }.containsAll(viewColumns.values)) {
          // Same view, add it as a full query result.
          val (indexedViewColumns, leftovers) = tableColumns(columns, viewColumns.values)
          methodNames.add(viewName)
          views.put(viewName, QueryResults.create(relativePath, viewColumns, symbolTable,
              originalViewName, true, tableIndex + indexedViewColumns.map { it.index }.min()!!,
              symbolTable.tableTypes[originalViewName]!!))
          expressions.addAll(leftovers)
        }
      }

      // Take the columns from incomplete tables in the resolution and add them to the expression list.
      expressions.addAll(columnsForTableName
          .filter { !tables.keys.contains(it.key) }
          .filter { !views.keys.contains(it.key) }
          .flatMap { it.value })

      // Add all the expressions in as individual columns.
      val columns = LinkedHashMap<String, IndexedValue>()
      for((index, value) in expressions) {
        val tableName = value.columnDef?.parentTable()?.table_name()?.text
        val indexedValue = IndexedValue(index, value, symbolTable.tableTypes[tableName], tableName)
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

      return QueryResults(relativePath, isView, queryName, columns, tables, views, tableIndex,
          modelInterface)
    }

    private fun <T: ParserRuleContext> Map<String, T>.matchTableName(
        aliasMap: Map<String, Set<IndexedValue>>, // Aliases to set of columns
        originalTables: Map<String, String>, // Map of aliases to true table names.
        operation: (String, T, String, Set<IndexedValue>) -> Unit
    ) {
      forEach { mapEntry ->
        val originalName = mapEntry.key
        val originalDefinition = mapEntry.value
        aliasMap.filterKeys { originalTables[it] == originalName }.forEach {
          operation(originalName, originalDefinition, it.key, it.value)
        }
      }
    }

    /**
     * Take a list of indexed values from a query and a list of values from a table/view and return
     * a pair where the first value is the list of indexed values for that table and the second
     * value is the list of indexed values left over.
     */
    private fun tableColumns(
        original: Collection<IndexedValue>,
        tableValues: List<Value>
    ): Pair<List<IndexedValue>, List<IndexedValue>> {
      val indexedTableValues = ArrayList<IndexedValue>()
      tableValues.forEach { tableValue ->
        indexedTableValues.add(original.first {
          !indexedTableValues.contains(it) && it.value == tableValue
        })
      }
      return indexedTableValues to original - indexedTableValues
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
      val tableInterface: ClassName?,
      val tableName: String?
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
      val indexedValues: Collection<IndexedValue>,
      val interfaceType: ClassName
  ) {
    val index = indexedValues.first().index
  }
}
