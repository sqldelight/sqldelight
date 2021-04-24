package com.squareup.sqldelight.core

import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.asClassName
import com.squareup.sqldelight.core.dialect.sqlite.SqliteType
import com.squareup.sqldelight.core.lang.util.argumentType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.isArrayParameter
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BindArgsTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `bind arg inherit name from column`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE data.id = ?;
      """.trimMargin(),
      tempFolder
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    val bindArgType = file.findChildrenOfType<SqlBindExpr>().first().argumentType()
    assertThat(bindArgType.dialectType).isEqualTo(SqliteType.INTEGER)
    assertThat(bindArgType.javaType).isEqualTo(List::class.asClassName())
    assertThat(bindArgType.name).isEqualTo("id")
    assertThat(bindArgType.column).isSameAs(column)
  }

  @Test fun `bind args inherit name from alias`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |CREATE VIEW data_aliased AS
      |SELECT id AS data_id
      |FROM data;
      |
      |selectForId:
      |SELECT *
      |FROM data_aliased
      |WHERE data_id = ?;
      """.trimMargin(),
      tempFolder
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    val bindArgType = file.findChildrenOfType<SqlBindExpr>().first().argumentType()
    assertThat(bindArgType.dialectType).isEqualTo(SqliteType.INTEGER)
    assertThat(bindArgType.javaType).isEqualTo(List::class.asClassName())
    assertThat(bindArgType.name).isEqualTo("data_id")
    assertThat(bindArgType.column).isSameAs(column)
  }

  @Test fun `bind args inherit names in insert statements`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (id)
      |VALUES (?), (?);
      """.trimMargin(),
      tempFolder
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args inherit names in default insert statements`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?), (?);
      """.trimMargin(),
      tempFolder
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args in compound select inherit type from compounded query`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |UNION
      |VALUES (?);
      """.trimMargin(),
      tempFolder
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args in parenthesis in compound select infers type from compounded query`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |UNION
      |VALUES (((?)));
      """.trimMargin(),
      tempFolder
    )

    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isNotNull()
    }
  }

  @Test fun `bind args for update statements inherit type from column`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someUpdate:
      |UPDATE data
      |SET id = ?
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args for upsert do update statements inherit type from column`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY NOT NULL,
      |  list INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someUpsert:
      |INSERT INTO data(
      |  id, list
      |)
      |VALUES(
      |  :id, :list
      |)
      |ON CONFLICT(
      |  id
      |)
      |DO UPDATE SET
      |  list = :list;
      """.trimMargin(),
      tempFolder, dialectPreset = DialectPreset.SQLITE_3_24
    )

    val (idColumn, listColumn) = file.findChildrenOfType<SqlColumnDef>().toList()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      when (it.name) {
        "id" -> {
          assertThat(it.dialectType).isEqualTo(SqliteType.INTEGER)
          assertThat(it.javaType).isEqualTo(Long::class.asClassName())
          assertThat(it.name).isEqualTo("id")
          assertThat(it.column).isSameAs(idColumn)
        }

        "list" -> {
          assertThat(it.dialectType).isEqualTo(SqliteType.INTEGER)
          assertThat(it.javaType).isEqualTo(List::class.asClassName())
          assertThat(it.name).isEqualTo("list")
          assertThat(it.column).isSameAs(listColumn)
        }
      }
    }
  }

  @Test fun `bind args inherit alias name`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM (
      |  SELECT id AS some_alias
      |  FROM data
      |)
      |WHERE some_alias = ?;
      """.trimMargin(),
      tempFolder
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("some_alias")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args for in statement inherit column name`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |WHERE id IN ?;
      """.trimMargin(),
      tempFolder
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameAs(column)
      assertThat(it.bindArg!!.isArrayParameter()).isTrue()
    }
  }

  @Test fun `bind args use proper binary operator precedence`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE User (
      |  type TEXT,
      |  first_name TEXT,
      |  last_name TEXT
      |);
      |
      |someSelect:
      |SELECT *
      |  FROM User
      | WHERE type = ?
      |   AND first_name LIKE ?
      |   AND last_name LIKE ?;
      """.trimMargin(),
      tempFolder
    )

    val columns = file.findChildrenOfType<SqlColumnDef>().toTypedArray()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.let { args ->
      assertThat(args[0].dialectType).isEqualTo(SqliteType.TEXT)
      assertThat(args[0].javaType).isEqualTo(String::class.asClassName().copy(nullable = true))
      assertThat(args[0].name).isEqualTo("type")
      assertThat(args[0].column).isEqualTo(columns[0])

      assertThat(args[1].dialectType).isEqualTo(SqliteType.TEXT)
      assertThat(args[1].javaType).isEqualTo(String::class.asClassName())
      assertThat(args[1].name).isEqualTo("first_name")

      assertThat(args[2].dialectType).isEqualTo(SqliteType.TEXT)
      assertThat(args[2].javaType).isEqualTo(String::class.asClassName())
      assertThat(args[2].name).isEqualTo("last_name")
    }
  }
}
