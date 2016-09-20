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

import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.SqliteParser.Select_stmtContext
import com.squareup.sqldelight.resolution.Resolver
import com.squareup.sqldelight.resolution.query.Result
import com.squareup.sqldelight.resolution.query.resultColumnSize
import com.squareup.sqldelight.resolution.resolve
import com.squareup.sqldelight.util.parse
import com.squareup.sqldelight.util.statementWithName
import org.junit.Test
import java.io.File

class ResolverTests {
  private val testFile = File("src/test/data/ResolverTestData.sq")
  private val parsed = parse(testFile)
  private val symbolTable = SymbolTable() + SymbolTable(parsed, parsed, testFile.path)
  private val resolver = Resolver(symbolTable)

  @Test
  fun selectAll() {
    val resolution = resolver.resolve(parsed.statementWithName("select_all") as Select_stmtContext)
    assertThat(resolution)
        .isSelected("_id", "test_column1", "test_column2", tableName = "test")
  }

  @Test
  fun selectCount() {
    val resolution = resolver.resolve(parsed.statementWithName("select_count") as Select_stmtContext)
    assertThat(resolution)
        .isSelected("count")
  }

  @Test
  fun selectFromSubquery() {
    val resolution = resolver.resolve(parsed.statementWithName("select_from_subquery") as Select_stmtContext)
    assertThat(resolution)
        .isSelected("_id", "test_column1", "test_column2", tableName = "test")
  }

  @Test
  fun selectCountFromSubquery() {
    val resolution = resolver.resolve(parsed.statementWithName("select_count_from_subquery") as Select_stmtContext)
    assertThat(resolution)
        .isSelected("count")
  }

  @Test
  fun subqueryInWhere() {
    val resolution = resolver.resolve(parsed.statementWithName("subquery_in_where") as Select_stmtContext)
    assertThat(resolution)
        .isSelected("_id", "test_column1", "test_column2", tableName = "test")
  }

  @Test
  fun selectFromValues() {
    val resolution = resolver.resolve(parsed.statementWithName("select_from_values") as Select_stmtContext)
    assertThat(resolution).hasResultSize(3)
  }

  @Test
  fun commaJoin() {
    val resolution = resolver.resolve(parsed.statementWithName("comma_join") as Select_stmtContext)
    assertThat(resolution).hasSelected("_id", "test")
        .hasSelected("test_column1", "test")
        .hasSelected("test_column2", "test")
        .hasResultSize(4) // The cheese column is unnamed.
  }

  @Test
  fun withQuery() {
    val resolution = resolver.resolve(parsed.statementWithName("with_query") as Select_stmtContext)
    assertThat(resolution).hasSelected("column1")
        .hasSelected("column2")
  }

  @Test
  fun types() {
    val resolution = resolver.resolve(parsed.statementWithName("types") as Select_stmtContext)
    assertThat(resolution)
        .hasType("count", SqliteType.INTEGER)
        .hasType("test_column1", SqliteType.INTEGER)
        .hasType("test_column2", SqliteType.TEXT)
        .hasType("abs", SqliteType.REAL)
        .hasType("abs2", SqliteType.INTEGER)
        .hasType("max1", SqliteType.INTEGER)
        .hasType("max2", SqliteType.INTEGER)
        .hasType("thirty", SqliteType.INTEGER)
        .hasType("multiple_type_max", SqliteType.BLOB)
        .hasType("multiple_type_min", SqliteType.NULL)
        .hasType("real_min", SqliteType.REAL)
  }

  private fun assertThat(values: List<Result>) = ValuesSubject(values)

  private class ValuesSubject(
      val values: List<Result>
  ) : IterableSubject(Truth.THROW_ASSERTION_ERROR, values) {
    fun isSelected(vararg columnNames: String, tableName: String? = null): ValuesSubject {
      columnNames.forEach { hasSelected(it, tableName) }
      assertThat(columnNames.size).named("table $tableName columns").isEqualTo(values.resultColumnSize())
      return this
    }

    fun hasSelected(columnName: String, tableName: String? = null): ValuesSubject {
      val selected = values.filter { it.findElement(columnName, tableName).isNotEmpty() }
      assertThat(selected).named("column $columnName in table $tableName").isNotEmpty()
      return this
    }

    fun hasType(columnName: String, type: SqliteType, tableName: String? = null): ValuesSubject {
      val selected = values.filter { it.findElement(columnName, tableName).isNotEmpty() }
      assertThat(selected).named("column $columnName in table $tableName").isNotEmpty()
      assertThat(selected[0].javaType).isEqualTo(type.defaultType)
      return this
    }

    fun hasResultSize(size: Int): ValuesSubject {
      assertThat(values.resultColumnSize()).isEqualTo(size)
      return this
    }
  }
}
