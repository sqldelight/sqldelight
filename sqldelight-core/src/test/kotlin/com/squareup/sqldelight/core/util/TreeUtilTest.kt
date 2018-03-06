package com.squareup.sqldelight.core.util

import com.google.common.truth.Truth.assertThat
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

    assertThat(createTable.rawSqlText()).isEqualTo("""
      |CREATE TABLE test (
      |  value TEXT,
      |  other_value TEXT,
      |  a_third_value INTEGER DEFAULT 0
      |)
    """.trimMargin())
  }
}