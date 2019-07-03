package com.squareup.sqldelight.core.util

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.lang.util.optimizeRawSqlText
import com.squareup.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TreeUtilTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun `rawSqlText removes AS modifier`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value TEXT AS kotlin.collections.List<Int>,
      |  other_value TEXT AS Int,
      |  a_third_value INTEGER AS Boolean DEFAULT 0
      |);
    """.trimMargin(), temporaryFolder)

    val createTable = file.sqliteStatements().first().statement.createTableStmt!!

    assertThat(createTable.rawSqlText()).isEqualTo("CREATE TABLE test (value TEXT, other_value TEXT, a_third_value INTEGER DEFAULT 0)")
  }

  @Test fun `rawSqlText replaces insert bind param with columns being inserted`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value TEXT AS kotlin.collections.List<Int>,
      |  other_value TEXT AS Int,
      |  a_third_value INTEGER AS Boolean DEFAULT 0
      |);
      |
      |INSERT INTO test (value, other_value)
      |VALUES ?;
      |
      |INSERT INTO test
      |VALUES ?;
    """.trimMargin(), temporaryFolder)

    val insert1 = file.sqliteStatements().elementAt(1).statement.insertStmt!!

    assertThat(insert1.rawSqlText()).isEqualTo("INSERT INTO test (value, other_value) VALUES (?, ?)")

    val insert2 = file.sqliteStatements().elementAt(2).statement.insertStmt!!

    assertThat(insert2.rawSqlText()).isEqualTo("INSERT INTO test VALUES (?, ?, ?)")
  }

    @Test fun `optimizeRawSqlText removes comments and extra whitespace`() {
        val createStatement = """
          |CREATE TABLE test (
          |  -- hello world
          |  value TEXT AS kotlin.collections.List<Int>,
          |  other_value TEXT AS Int,
          |  a_third_value INTEGER AS Boolean DEFAULT 0 -- inlined comment
          |  -- another comment
          |);
        """.trimMargin()

        assertThat(optimizeRawSqlText(createStatement)).isEqualTo("CREATE TABLE test (value TEXT AS kotlin.collections.List<Int>, other_value TEXT AS Int, a_third_value INTEGER AS Boolean DEFAULT 0);")

    }
}