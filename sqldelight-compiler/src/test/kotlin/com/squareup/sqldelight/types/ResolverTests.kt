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
import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqliteParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class ResolverTests {
  private val parsed = parse(File("src/test/data/ResolverTestData.sq"))
  private val symbolTable = SymbolTable() + SymbolTable(parsed, parsed)
  private val resolver = Resolver(symbolTable)

  @Test
  fun selectAll() {
    val resolution = resolver.resolve(parsed.statementWithName("select_all"))
    assertThat(resolution.values)
        .isSelected("_id", "test_column1", "test_column2", tableName = "test")
  }

  @Test
  fun selectCount() {
    val resolution = resolver.resolve(parsed.statementWithName("select_count"))
    assertThat(resolution.values)
        .isSelected("count")
  }

  @Test
  fun selectFromSubquery() {
    val resolution = resolver.resolve(parsed.statementWithName("select_from_subquery"))
    assertThat(resolution.values)
        .isSelected("_id", "test_column1", "test_column2", tableName = "test")
  }

  @Test
  fun selectCountFromSubquery() {
    val resolution = resolver.resolve(parsed.statementWithName("select_count_from_subquery"))
    assertThat(resolution.values)
        .isSelected("count")
  }

  @Test
  fun subqueryInWhere() {
    val resolution = resolver.resolve(parsed.statementWithName("subquery_in_where"))
    assertThat(resolution.values)
        .isSelected("_id", "test_column1", "test_column2", tableName = "test")
  }

  @Test
  fun selectFromValues() {
    val resolution = resolver.resolve(parsed.statementWithName("select_from_values"))
    assertThat(resolution.values).hasSize(3)
  }

  fun parse(file: File): SqliteParser.ParseContext {
    FileInputStream(file).use { inputStream ->
      val lexer = SqliteLexer(ANTLRInputStream(inputStream))
      lexer.removeErrorListeners()

      val parser = SqliteParser(CommonTokenStream(lexer))
      parser.removeErrorListeners()
      return parser.parse()
    }
  }

  @Test
  fun commaJoin() {
    val resolution = resolver.resolve(parsed.statementWithName("comma_join"))
    assertThat(resolution.values).hasSelected("_id", "test")
        .hasSelected("test_column1", "test")
        .hasSelected("test_column2", "test")
        .hasSize(4) // The cheese column is unnamed.
  }

  @Test
  fun withQuery() {
    val resolution = resolver.resolve(parsed.statementWithName("with_query"))
    assertThat(resolution.values).hasSelected("column1", "temp_table1")
        .hasSelected("column2", "temp_table2")
  }

  private fun assertThat(values: List<Value>) = ValuesSubject(values)

  private fun SqliteParser.ParseContext.statementWithName(name: String): ParserRuleContext {
    val child = sql_stmt_list().sql_stmt().find({ it.sql_stmt_name().text == name })
    return child?.getChild(child.childCount - 1) as ParserRuleContext
  }

  private class ValuesSubject(
      val values: List<Value>
  ) : IterableSubject<ValuesSubject, Value, List<Value>>(Truth.THROW_ASSERTION_ERROR, values) {
    fun isSelected(vararg columnNames: String, tableName: String? = null): ValuesSubject {
      columnNames.forEach { hasSelected(it, tableName) }
      assertThat(columnNames.size).named("table $tableName columns").isEqualTo(values.size)
      return this
    }

    fun hasSelected(columnName: String, tableName: String? = null): ValuesSubject {
      val selected = values.filter {
        it.columnName != null && it.columnName == columnName
            && (tableName == null || it.tableName == tableName)
      }
      assertThat(selected).named("column $columnName in table $tableName").isNotEmpty()
      return this
    }
  }
}