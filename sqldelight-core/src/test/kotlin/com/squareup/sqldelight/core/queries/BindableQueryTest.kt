package com.squareup.sqldelight.core.queries

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.asClassName
import com.squareup.sqldelight.core.compiler.model.namedMutators
import com.squareup.sqldelight.core.compiler.model.namedQueries
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.INTEGER
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.TEXT
import com.squareup.sqldelight.core.lang.psi.ColumnDefMixin
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BindableQueryTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `arguments with the same index are reused`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |selectForStuff:
      |SELECT *
      |FROM data
      |WHERE _id = ?1 AND _id = ?1;
      """.trimMargin(), tempFolder)

    val createTable = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()
    val select = file.sqliteStatements().namedQueries().first()

    assertThat(select.arguments).containsExactly(
        1 to IntermediateType(INTEGER, LONG, createTable.columnDefList[0] as ColumnDefMixin, "_id")
    )
  }

  @Test fun `argument indexed to an already-used index is reused`() {
     val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |selectForStuff:
      |SELECT *
      |FROM data
      |WHERE _id = ? AND _id = ?1;
      """.trimMargin(), tempFolder)

    val createTable = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()
    val select = file.sqliteStatements().namedQueries().first()

    assertThat(select.arguments).containsExactly(
        1 to IntermediateType(INTEGER, LONG, createTable.column(0), "_id")
    )
  }

  @Test fun `auto-index takes next available index`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |selectForStuff:
      |SELECT *
      |FROM data
      |WHERE _id = ?20 AND value = ?;
      """.trimMargin(), tempFolder)

    val createTable = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()
    val select = file.sqliteStatements().namedQueries().first()

    assertThat(select.arguments).containsExactly(
        20 to IntermediateType(INTEGER, LONG, createTable.column(0), "_id"),
        21 to IntermediateType(TEXT, List::class.asClassName().asNullable(), createTable.column(1), "value")
    )
  }

  @Test fun `auto-generated parameter name conflicts with user-specified name`() {
     val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |selectForStuff:
      |SELECT *
      |FROM data
      |WHERE _id = :value AND value = ?;
      """.trimMargin(), tempFolder)

    val createTable = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()
    val select = file.sqliteStatements().namedQueries().first()

    assertThat(select.arguments).containsExactly(
        1 to IntermediateType(INTEGER, LONG, createTable.column(0), "value"),
        2 to IntermediateType(TEXT, List::class.asClassName().asNullable(), createTable.column(1), "value_")
    )
  }

  @Test fun `auto-generated parameter from earlier in query has conflicting name with user-specified name`() {
      val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |updateStuff:
      |UPDATE data
      |SET value = ?
      |WHERE value = :value;
      """.trimMargin(), tempFolder)

    val createTable = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()
    val update = file.sqliteStatements().namedMutators().first()

    assertThat(update.arguments).containsExactly(
        1 to IntermediateType(TEXT, List::class.asClassName().asNullable(), createTable.column(1), "value_"),
        2 to IntermediateType(TEXT, List::class.asClassName().asNullable(), createTable.column(1), "value")
    )
  }

  private fun SqliteCreateTableStmt.column(index: Int) = columnDefList[index] as ColumnDefMixin
}