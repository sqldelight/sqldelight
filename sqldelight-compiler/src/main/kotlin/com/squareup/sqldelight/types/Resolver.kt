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
package com.squareup.sqldelight.types

import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.types.ResolutionError.IncompleteRule
import com.squareup.sqldelight.validation.JoinValidator
import com.squareup.sqldelight.validation.ResultColumnValidator
import com.squareup.sqldelight.validation.SelectOrValuesValidator
import com.squareup.sqldelight.validation.SelectStmtValidator
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.antlr.v4.runtime.ParserRuleContext
import java.util.LinkedHashSet

/**
 * The only job of this class is to return an ordered list of values that any given
 * rule evaluates to. It will perform validation on subqueries to make sure they are well
 * formed before returning the selected columns.
 */
class Resolver(
    internal val symbolTable: SymbolTable,
    internal val dependencies: LinkedHashSet<Any> = linkedSetOf<Any>(),
    private val scopedValues: List<Value> = emptyList()
) {
  data class Response(
      val values: List<Value> = emptyList(),
      val errors: List<ResolutionError> = emptyList()
  ) {
    internal constructor(error: ResolutionError): this(errors = listOf(error))

    operator fun plus(other: Response) = Response(values + other.values, errors + other.errors)
  }

  val currentlyResolvingViews = linkedSetOf<String>()

  internal fun withResolver(with: SqliteParser.With_clauseContext) =
      Resolver(with.common_table_expression().fold(symbolTable, { symbolTable, commonTable ->
        symbolTable + SymbolTable(commonTable, commonTable)
      }), dependencies, scopedValues)

  /**
   * Take an insert statement and return the types being inserted.
   */
  fun resolve(insertStmt: SqliteParser.Insert_stmtContext, availableValues: List<Value>): Response {
    val resolver: Resolver
    if (insertStmt.with_clause() != null) {
      try {
        resolver = withResolver(insertStmt.with_clause())
      } catch (e: SqlitePluginException) {
        return Response(ResolutionError.WithTableError(e.originatingElement, e.message))
      }
    } else {
      resolver = this
    }

    if (insertStmt.values() != null) {
      return resolver.resolve(insertStmt.values(), availableValues)
    }
    if (insertStmt.select_stmt() != null) {
      return resolver.resolve(insertStmt.select_stmt())
    }
    if (insertStmt.K_DEFAULT() != null) {
      return Response()
    }

    return Response(ResolutionError.InsertError(insertStmt,
        "Did not know how to resolve insert statement $insertStmt"))
  }

  /**
   * Take a select statement and return the selected columns.
   */
  fun resolve(selectStmt: SqliteParser.Select_stmtContext): Response {
    val resolver: Resolver
    if (selectStmt.with_clause() != null) {
      try {
        resolver = withResolver(selectStmt.with_clause())
      } catch (e: SqlitePluginException) {
        return Response(ResolutionError.WithTableError(e.originatingElement, e.message))
      }
    } else {
      resolver = this
    }

    var resolution = resolver.resolve(selectStmt.select_or_values(0), selectStmt)

    // Resolve other compound select statements and verify they have equivalent columns.
    selectStmt.select_or_values().drop(1).forEach {
      val compoundValues = resolver.resolve(it)
      if (compoundValues.values.size != resolution.values.size) {
        resolution += Response(ResolutionError.CompoundError(it,
            "Unexpected number of columns in compound statement found: " +
                "${compoundValues.values.size} expected: ${resolution.values.size}"))
      }
      // TODO: Type checking.
      //for (valueIndex in 0..values.size) {
      //  if (values[valueIndex].type != compoundValues[valueIndex].type) {
      //    throw SqlitePluginException(compoundValues[valueIndex].element, "Incompatible types in " +
      //        "compound statement for column 2 found: ${compoundValues[valueIndex].type} " +
      //        "expected: ${values[valueIndex].type}")
      //  }
      //}
    }

    return resolution
  }

  /**
   * Takes a select_or_values rule and returns the columns selected.
   */
  fun resolve(
      selectOrValues: SqliteParser.Select_or_valuesContext,
      parentSelect: SqliteParser.Select_stmtContext? = null
  ): Response {
    var resolution: Response
    if (selectOrValues.K_VALUES() != null) {
      // No columns are available, only selected columns are returned.
      return Response(errors = SelectOrValuesValidator(this, scopedValues)
          .validate(selectOrValues)) + resolve(selectOrValues.values())
    } else if (selectOrValues.join_clause() != null) {
      resolution = resolve(selectOrValues.join_clause())
    } else if (selectOrValues.table_or_subquery().size > 0) {
      resolution = selectOrValues.table_or_subquery().foldRight(Response()) {
        table_or_subquery, response -> response + resolve(table_or_subquery)
      }
    } else {
      return Response(IncompleteRule(selectOrValues, "Missing table or subquery"))
    }

    // Validate the select or values has valid expressions before aliasing/selection.
    resolution += Response(errors = SelectOrValuesValidator(this, scopedValues + resolution.values)
        .validate(selectOrValues))

    if (parentSelect != null) {
      resolution += Response(errors = SelectStmtValidator(this, scopedValues + resolution.values)
          .validate(parentSelect))
    }

    return selectOrValues.result_column().foldRight(Response(errors = resolution.errors)) {
      result_column, response -> response + resolve(result_column, resolution.values)
    }
  }

  /**
   * Takes a value rule and returns the columns introduced. Validates that any
   * appended values have the same length.
   */
  fun resolve(
      values: SqliteParser.ValuesContext,
      availableValues: List<Value> = emptyList()
  ): Response {
    var selected = values.expr().foldRight(Response(), { expression, response ->
      response + resolve(expression, availableValues)
    })
    if (values.values() != null) {
      val joinedValues = resolve(values.values())
      selected += Response(errors = joinedValues.errors)
      if (joinedValues.values.size != selected.values.size) {
        selected += Response(ResolutionError.ValuesError(values.values(),
            "Unexpected number of columns in values found: ${joinedValues.values.size} " +
                "expected: ${selected.values.size}"))
      }
      // TODO: Type check
    }
    return selected
  }

  /**
   * Take in a list of available columns and return a list of selected columns.
   */
  fun resolve(
      resultColumn: SqliteParser.Result_columnContext,
      availableValues: List<Value>
  ): Response {
    // Like joins, the columns available after the select statement may change (due to aliasing)
    // so validation must happen BEFORE aliasing has occurred.
    ResultColumnValidator(this, availableValues).validate(resultColumn)

    if (resultColumn.text.equals("*")) {
      // SELECT *
      return Response(values = availableValues)
    }
    if (resultColumn.table_name() != null) {
      // SELECT some_table.*
      return Response(values = availableValues.filter {
        it.tableName == resultColumn.table_name().text
      })
    }
    if (resultColumn.expr() != null) {
      // SELECT expr
      var response = resolve(resultColumn.expr(), availableValues)
      if (resultColumn.column_alias() != null) {
        response = Response(response.values.map {
          Value(it.tableName, resultColumn.column_alias().text, it.type, it.element)
        }, response.errors)
      }
      return response
    }

    return Response(IncompleteRule(resultColumn, "Result set requires at least one column"))
  }

  /**
   * Takes a list of available values and returns a selected value.
   */
  fun resolve(expression: SqliteParser.ExprContext, availableValues: List<Value>): Response {
    if (expression.column_name() != null) {
      // | ( ( database_name '.' )? table_name '.' )? column_name
      val matchingColumns = availableValues.columns(expression.column_name().text,
          expression.table_name()?.text)
      if (matchingColumns.isEmpty()) {
        return Response(ResolutionError.ColumnOrTableNameNotFound(
            expression,
            "No column found with name ${expression.column_name().text}",
            availableValues,
            expression.table_name()?.text
        ))
      } else if (matchingColumns.size > 1) {
        return Response(ResolutionError.ExpressionError(
            expression,
            "Ambiguous column name ${expression.column_name().text}, " +
                "found in tables ${matchingColumns.map { it.tableName }}"
        ))
      } else {
        return Response(matchingColumns)
      }
    }

    // TODO get the actual type of the expression. Thats gonna be fun. :(
    return Response(values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression)))
  }

  /**
   * Take a join rule and return a list of the available columns.
   * Join rules look like
   *   FROM table_a JOIN table_b ON table_a.column_a = table_b.column_a
   */
  fun resolve(joinClause: SqliteParser.Join_clauseContext): Response {
    // Joins are complex because they are in a partial resolution state: They know about
    // values up to the point of this join but not afterward. Because of this, a validation step
    // for joins must happen as part of the resolution step.

    // Grab the values from the initial table or subquery (table_a in javadoc)
    var response = resolve(joinClause.table_or_subquery(0))

    joinClause.table_or_subquery().drop(1).zip(joinClause.join_constraint()) { table, constraint ->
      var localResponse = resolve(table)
      localResponse += Response(
          errors = JoinValidator(this, localResponse.values, response.values + scopedValues)
              .validate(constraint)
      )
      response += localResponse
    }
    return response
  }

  /**
   * Take a table or subquery rule and return a list of the selected values.
   */
  fun resolve(tableOrSubquery: SqliteParser.Table_or_subqueryContext): Response {
    var resolution: Response
    if (tableOrSubquery.table_name() != null) {
      resolution = resolve(tableOrSubquery.table_name())
    } else if (tableOrSubquery.select_stmt() != null) {
      resolution = resolve(tableOrSubquery.select_stmt())
    } else if (tableOrSubquery.table_or_subquery().size > 0) {
      resolution = tableOrSubquery.table_or_subquery().foldRight(
          Response()) { table_or_subquery, response ->
        response + resolve(table_or_subquery)
      }
    } else if (tableOrSubquery.join_clause() != null) {
      resolution = resolve(tableOrSubquery.join_clause())
    } else {
      return Response(IncompleteRule(tableOrSubquery, "Missing table or subquery"))
    }

    // Alias the values if an alias was given.
    if (tableOrSubquery.table_alias() != null) {
      resolution = resolution.copy(resolution.values.map {
        Value(tableOrSubquery.table_alias().text, it.columnName, it.type, it.element)
      })
    }

    return resolution
  }

  fun resolve(createTable: SqliteParser.Create_table_stmtContext) =
      Response(createTable.column_def().map { Value(createTable.table_name().text, it) })

  fun resolve(parserRuleContext: ParserRuleContext): Response {
    when (parserRuleContext) {
      is SqliteParser.Table_or_subqueryContext -> return resolve(parserRuleContext)
      is SqliteParser.Join_clauseContext -> return resolve(parserRuleContext)
      is SqliteParser.Select_stmtContext -> return resolve(parserRuleContext)
      is SqliteParser.Select_or_valuesContext -> return resolve(parserRuleContext)
    }
    val tableName = parserRuleContext
    val createTable = symbolTable.tables[tableName.text]
    if (createTable != null) {
      dependencies.add(symbolTable.tableTags.getForValue(tableName.text))
      if (createTable.select_stmt() != null) {
        return resolve(createTable.select_stmt())
      }
      return resolve(createTable)
    }

    val view = symbolTable.views[tableName.text]
    if (view != null) {
      dependencies.add(symbolTable.viewTags.getForValue(tableName.text))
      if (!currentlyResolvingViews.add(view.view_name().text)) {
        val chain = currentlyResolvingViews.joinToString(" -> ")
        return Response(ResolutionError.RecursiveResolution(view.view_name(),
            "Recursive subquery found: $chain -> ${view.view_name().text}"))
      }
      val originalResult = resolve(view.select_stmt())
      val result = originalResult.copy(values = originalResult.values.map {
        Value(view.view_name().text, it.columnName, it.type, it.element)
      })
      currentlyResolvingViews.remove(view.view_name().text)
      return result
    }

    val commonTable = symbolTable.commonTables[tableName.text]
    if (commonTable != null) {
      var resolution = resolve(commonTable.select_stmt())
      if (commonTable.column_name().size > 0) {
        // Keep the errors from the original resolution but only the values that
        // are specified in the column_name() array.
        resolution = Response(errors = resolution.errors) + commonTable.column_name()
            .foldRight(Response()) { column_name, response ->
              val found = resolution.values.columns(column_name.text, null)
              if (found.size == 0) {
                return response + Response(ResolutionError.ColumnNameNotFound(
                    column_name,
                    "No column found in common table with name ${column_name.text}",
                    resolution.values
                ))
              }
              val originalResponse = Response(found)
              return response + originalResponse.copy(values = originalResponse.values.map {
                Value(tableName.text, it.columnName, it.type, it.element)
              })
            }
      }
      return resolution.copy(values = resolution.values.map {
        Value(tableName.text, it.columnName, it.type, it.element)
      })
    }

    // If table was missing we add a dependency on all future files.
    dependencies.add(SqlDelightValidator.ALL_FILE_DEPENDENCY)

    return Response(ResolutionError.TableNameNotFound(tableName,
        "Cannot find table or view ${tableName.text}",
        symbolTable.commonTables.keys + symbolTable.tables.keys + symbolTable.views.keys
    ))
  }

  fun foreignKeys(foreignTable: SqliteParser.Foreign_tableContext): ForeignKey {
    return ForeignKey.findForeignKeys(foreignTable, symbolTable, resolve(foreignTable).values)
  }
}