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

import com.google.common.truth.Subject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.resolution.query.QueryResults
import com.squareup.sqldelight.resolution.query.Table
import com.squareup.sqldelight.resolution.query.Value
import com.squareup.sqldelight.util.parse
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.junit.Test
import java.io.File

class ValidatorTests {
  private val testFile = File("src/test/data/ResolverTestData.sq")
  private val parsed = parse(testFile)
  private val symbolTable = SymbolTable() + SymbolTable(parsed, testFile, testFile.path)
  private val status = SqlDelightValidator()
      .validate(testFile.path, parsed, symbolTable) as Status.ValidationStatus.Validated

  @Test
  fun selectAll() {
    assertThat(status.queries.withName("select_all"))
        .hasTable("test", 0, 1, 2)
        .hasSize(1, 0)
  }

  @Test
  fun selectCount() {
    assertThat(status.queries.withName("select_count"))
        .hasColumn("count", SqliteType.INTEGER, 0)
        .hasSize(0, 1)
  }

  @Test
  fun selectFromSubquery() {
    assertThat(status.queries.withName("select_from_subquery"))
        .hasTable("test", 0, 1, 2)
        .hasSize(1, 0)
  }

  @Test
  fun selectCountFromSubquery() {
    assertThat(status.queries.withName("select_count_from_subquery"))
        .hasColumn("count", SqliteType.INTEGER, 0)
        .hasSize(0, 1)
  }

  @Test
  fun selectFromValues() {
    assertThat(status.queries.withName("select_from_values"))
        .hasColumn("int_literal", SqliteType.INTEGER, 0)
        .hasColumn("int_literal_2", SqliteType.INTEGER, 1)
        .hasColumn("int_literal_3", SqliteType.INTEGER, 2)
        .hasSize(0, 3)
  }

  @Test
  fun withQuery() {
    assertThat(status.queries.withName("with_query"))
        .hasColumn("column1", SqliteType.INTEGER, 0)
        .hasColumn("column2", SqliteType.INTEGER, 1)
        .hasSize(0, 2)
  }

  @Test
  fun types() {
    assertThat(status.queries.withName("types"))
        .hasColumn("count", SqliteType.INTEGER, 0)
        .hasColumn("test_column1", SqliteType.INTEGER, 1)
        .hasColumn("test_column2", SqliteType.TEXT, 2)
        .hasColumn("abs", SqliteType.REAL, 3)
        .hasColumn("abs2", SqliteType.INTEGER, 4)
        .hasColumn("max1", SqliteType.INTEGER, 5)
        .hasColumn("max2", SqliteType.INTEGER, 6)
        .hasColumn("thirty", SqliteType.INTEGER, 7)
        .hasColumn("multiple_type_max", SqliteType.BLOB, 8)
        .hasColumn("multiple_type_min", SqliteType.NULL, 9)
        .hasColumn("real_min", SqliteType.REAL, 10)
        .hasColumn("avg", SqliteType.REAL, 11)
        .hasSize(0, 12)
  }

  @Test
  fun multipleTables() {
    assertThat(status.queries.withName("multiple_tables"))
        .hasTable("test1", 0, 1, 2)
        .hasTable("test2", 3, 4, 5)
        .hasSize(2, 0)
  }

  @Test
  fun multipleTables2() {
    assertThat(status.queries.withName("multiple_tables_2"))
        .hasTable("test1", 0, 1, 2)
        .hasTable("test2", 3, 4, 5)
        .hasSize(2, 0)
  }

  @Test
  fun tablesAndColumns() {
    assertThat(status.queries.withName("tables_and_columns"))
        .hasTable("test1", 0, 1, 2)
        .hasTable("test2", 3, 4, 5)
        .hasColumn("count_test1__id", SqliteType.INTEGER, 6)
        .hasColumn("test_column2", SqliteType.INTEGER, 7)
        .hasSize(2, 2)
  }

  @Test
  fun tablesAndColumnsReversed() {
    assertThat(status.queries.withName("tables_and_columns_reversed"))
        .hasTable("test1", 1, 2, 3)
        .hasTable("test2", 4, 5, 6)
        .hasColumn("count_test1__id", SqliteType.INTEGER, 0)
        .hasSize(2, 1)
  }

  @Test
  fun selectFromView() {
    assertThat(status.queries.withName("select_from_view").results.filterIsInstance<QueryResults>()).hasSize(1)
    val view = status.queries.withName("select_from_view").results.filterIsInstance<QueryResults>().first()
    assertThat(view.name).isEqualTo("cheese")

    assertThat(view)
        .hasColumn("string_literal", SqliteType.TEXT, 0)
        .hasSize(0, 1)
  }

  private fun assertThat(queryResults: QueryResults) = QueryResultsSubject(queryResults)

  private class QueryResultsSubject(
      val queryResults: QueryResults
  ): Subject<QueryResultsSubject, QueryResults>(Truth.THROW_ASSERTION_ERROR, queryResults) {
    fun hasTable(tableName: String, vararg indices: Int): QueryResultsSubject {
      var index = 0
      val (tableIndex, table) = queryResults.results
          .map { result ->
            val pair = index to result
            index += result.size()
            return@map pair
          }
          .filter { it.second is Table && it.second.name == tableName }
          .first()
      assertThat(table).isNotNull()
      tableIndex.rangeTo(tableIndex + table.size() - 1).forEachIndexed { index, valueIndex ->
        assertThat(indices[index]).isEqualTo(valueIndex)
      }
      return this
    }

    fun hasColumn(columnName: String, type: SqliteType, index: Int): QueryResultsSubject {
      var currentIndex = 0
      val (columnIndex, column) = queryResults.results
          .map { result ->
            val pair = currentIndex to result
            currentIndex += result.size()
            return@map pair
          }
          .filter { it.second is Value && it.second.name == columnName }
          .first()
      assertThat(column).isNotNull()
      assertThat(column.javaType).isEqualTo(type.defaultType)
      assertThat(columnIndex).isEqualTo(index)
      return this
    }

    fun hasSize(tableSize: Int, columnSize: Int): QueryResultsSubject {
      assertThat(queryResults.results.filterIsInstance<Table>().size).isEqualTo(tableSize)
      assertThat(queryResults.results.filterIsInstance<Value>().size).isEqualTo(columnSize)
      return this
    }
  }

  private fun List<QueryResults>.withName(name: String) = first { it.name == name }
}
