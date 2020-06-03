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

    assertThat(insert1.rawSqlText()).isEqualTo("""
      |INSERT INTO test (value, other_value)
      |VALUES (?, ?)
    """.trimMargin())

    val insert2 = file.sqliteStatements().elementAt(2).statement.insertStmt!!

    assertThat(insert2.rawSqlText()).isEqualTo("""
      |INSERT INTO test
      |VALUES (?, ?, ?)
    """.trimMargin())
  }

  @Test fun `rawSqlText removes type for fts5`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE VIRTUAL TABLE data USING fts5 (
      |  value TEXT,
      |  prefix='2 3 4 5 6'
      |);
    """.trimMargin(), temporaryFolder)

    val createTable = file.sqliteStatements().first().statement.createVirtualTableStmt!!

    assertThat(createTable.rawSqlText()).isEqualTo("""
      |CREATE VIRTUAL TABLE data USING fts5 (
      |  value,
      |  prefix='2 3 4 5 6'
      |)
    """.trimMargin())
  }

  @Test fun `rawSqlText removes type and constraints for fts5`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE VIRTUAL TABLE data USING fts5 (
      |  value TEXT NOT NULL,
      |  prefix='2 3 4 5 6'
      |);
    """.trimMargin(), temporaryFolder)

    val createTable = file.sqliteStatements().first().statement.createVirtualTableStmt!!

    assertThat(createTable.rawSqlText()).isEqualTo("""
      |CREATE VIRTUAL TABLE data USING fts5 (
      |  value,
      |  prefix='2 3 4 5 6'
      |)
    """.trimMargin())
  }
}
