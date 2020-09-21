package com.squareup.sqldelight.core.queries

import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.google.common.truth.Truth.assertThat
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.asClassName
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.INTEGER
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.TEXT
import com.squareup.sqldelight.test.util.FixtureCompiler
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
    val select = file.namedQueries.first()
    val arg = PsiTreeUtil.findChildrenOfType(file, SqlBindExpr::class.java).first()

    assertThat(select.arguments.map { it.index to it.type }).containsExactly(
      1 to IntermediateType(INTEGER, LONG, createTable.columnDefList[0], "_id", arg)
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
    val select = file.namedQueries.first()
    val arg = PsiTreeUtil.findChildrenOfType(file, SqlBindExpr::class.java).first()

    assertThat(select.arguments.map { it.index to it.type }).containsExactly(
      1 to IntermediateType(INTEGER, LONG, createTable.columnDefList[0], "_id", arg)
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
    val select = file.namedQueries.first()
    val args = PsiTreeUtil.findChildrenOfType(file, SqlBindExpr::class.java).toTypedArray()

    assertThat(select.arguments.map { it.index to it.type }).containsExactly(
      20 to IntermediateType(INTEGER, LONG, createTable.columnDefList[0], "_id", args[0]),
      21 to IntermediateType(TEXT, List::class.asClassName().copy(nullable = true),
          createTable.columnDefList[1], "value", args[1])
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
    val select = file.namedQueries.first()
    val args = PsiTreeUtil.findChildrenOfType(file, SqlBindExpr::class.java).toTypedArray()

    assertThat(select.arguments.map { it.index to it.type }).containsExactly(
      1 to IntermediateType(INTEGER, LONG, createTable.columnDefList[0], "value", args[0]),
      2 to IntermediateType(TEXT, List::class.asClassName().copy(nullable = true),
          createTable.columnDefList[1], "value_", args[1])
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
    val update = file.namedMutators.first()
    val args = PsiTreeUtil.findChildrenOfType(file, SqlBindExpr::class.java).toTypedArray()

    assertThat(update.arguments.map { it.index to it.type }).containsExactly(
      1 to IntermediateType(TEXT, List::class.asClassName().copy(nullable = true),
          createTable.columnDefList[1], "value_", args[0]),
      2 to IntermediateType(TEXT, List::class.asClassName().copy(nullable = true),
          createTable.columnDefList[1], "value", args[1])
    )
  }

  @Test fun `argument in ifnull statement`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE filtered (
      |  filtered TEXT,
      |  bufferId INTEGER,
      |  accountId INTEGER
      |);
      |
      |updateStuff:
      |SELECT IFNULL(t.filtered, :defaultValue)
      |FROM (
      |    SELECT filtered
      |    FROM filtered
      |    WHERE bufferId = :bufferId
      |    AND accountId = :accountId
      |    UNION
      |    SELECT NULL
      |    ORDER BY filtered
      |    DESC LIMIT 1
      |) t;
      """.trimMargin(), tempFolder)

    val createTable = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()
    val select = file.namedQueries.first()
    val args = PsiTreeUtil.findChildrenOfType(file, SqlBindExpr::class.java).toTypedArray()

    assertThat(select.arguments.map { it.index to it.type }).containsExactly(
      1 to IntermediateType(TEXT, String::class.asClassName().copy(nullable = true), null, "defaultValue", args[0]),
      2 to IntermediateType(INTEGER, Long::class.asClassName().copy(nullable = true),
          createTable.columnDefList[1], "bufferId", args[1]),
      3 to IntermediateType(INTEGER, Long::class.asClassName().copy(nullable = true),
          createTable.columnDefList[2], "accountId", args[2])
    )
  }
}
