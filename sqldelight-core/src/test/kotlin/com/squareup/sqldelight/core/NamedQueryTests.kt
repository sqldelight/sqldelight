package com.squareup.sqldelight.core

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.model.namedQueries
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NamedQueryTests {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `tablesObserved returns a list of all tables observed`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE _id = ?;
      """.trimMargin(), tempFolder)

    val query = file.sqliteStatements().namedQueries().first()
    val table = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()

    assertThat(query.tablesObserved).containsExactly(table)
  }

  @Test fun `tablesObserved resolves table aliases properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT data2.*
      |FROM data AS data2
      |WHERE data2._id = ?;
      """.trimMargin(), tempFolder)

    val query = file.sqliteStatements().namedQueries().first()
    val table = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()

    assertThat(query.tablesObserved).containsExactly(table)
  }

  @Test fun `tablesObserved resolves views properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |CREATE VIEW some_view AS
      |SELECT *
      |FROM data;
      |
      |selectForId:
      |SELECT *
      |FROM some_view;
      """.trimMargin(), tempFolder)

    val query = file.sqliteStatements().namedQueries().first()
    val table = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()

    assertThat(query.tablesObserved).containsExactly(table)
  }

  @Test fun `tablesObserved resolves common tables properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |WITH common_table AS (
      |  SELECT *
      |  FROM data
      |)
      |SELECT *
      |FROM common_table;
      """.trimMargin(), tempFolder)

    val query = file.sqliteStatements().namedQueries().first()
    val table = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()

    assertThat(query.tablesObserved).containsExactly(table)
  }
}