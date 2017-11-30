package com.squareup.sqldelight.core

import com.alecstrong.sqlite.psi.core.psi.SqliteBindExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnDef
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.util.argumentType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BindArgsTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `bind arg inherit name from column`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE data._id = ?;
      """.trimMargin(), tempFolder)

    val column = file.findChildrenOfType<SqliteColumnDef>().first()
    val bindArgType = file.findChildrenOfType<SqliteBindExpr>().first().argumentType()
    assertThat(bindArgType.sqliteType).isEqualTo(IntermediateType.SqliteType.INTEGER)
    assertThat(bindArgType.javaType).isEqualTo(List::class.asClassName())
    assertThat(bindArgType.name).isEqualTo("_id")
    assertThat(bindArgType.column).isSameAs(column)
  }

  @Test fun `bind args inherit name from alias`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |CREATE VIEW data_aliased AS
      |SELECT _id AS data_id
      |FROM data;
      |
      |selectForId:
      |SELECT *
      |FROM data_aliased
      |WHERE data_id = ?;
      """.trimMargin(), tempFolder)

    val column = file.findChildrenOfType<SqliteColumnDef>().first()
    val bindArgType = file.findChildrenOfType<SqliteBindExpr>().first().argumentType()
    assertThat(bindArgType.sqliteType).isEqualTo(IntermediateType.SqliteType.INTEGER)
    assertThat(bindArgType.javaType).isEqualTo(List::class.asClassName())
    assertThat(bindArgType.name).isEqualTo("data_id")
    assertThat(bindArgType.column).isSameAs(column)
  }

  @Test fun `bind args inherit names in insert statements`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (_id)
      |VALUES (?), (?);
      """.trimMargin(), tempFolder)

    val column = file.findChildrenOfType<SqliteColumnDef>().first()
    file.findChildrenOfType<SqliteBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.sqliteType).isEqualTo(IntermediateType.SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("_id")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args inherit names in default insert statements`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?), (?);
      """.trimMargin(), tempFolder)

    val column = file.findChildrenOfType<SqliteColumnDef>().first()
    file.findChildrenOfType<SqliteBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.sqliteType).isEqualTo(IntermediateType.SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("_id")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args in compound select inherit type from compounded query`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |UNION
      |VALUES (?);
      """.trimMargin(), tempFolder)

    val column = file.findChildrenOfType<SqliteColumnDef>().first()
    file.findChildrenOfType<SqliteBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.sqliteType).isEqualTo(IntermediateType.SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("_id")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args in parenthesis in compound select loses type from compounded query`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |UNION
      |VALUES (((?)));
      """.trimMargin(), tempFolder)

    file.findChildrenOfType<SqliteBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.sqliteType).isEqualTo(IntermediateType.SqliteType.ARGUMENT)
      assertThat(it.javaType).isEqualTo(Any::class.asClassName())
      assertThat(it.name).isEqualTo("value")
      assertThat(it.column).isNull()
    }
  }

  @Test fun `bind args for update statements inherit type from column`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someUpdate:
      |UPDATE data
      |SET _id = ?
      |WHERE _id = ?;
      """.trimMargin(), tempFolder)

    val column = file.findChildrenOfType<SqliteColumnDef>().first()
    file.findChildrenOfType<SqliteBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.sqliteType).isEqualTo(IntermediateType.SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("_id")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args inherit alias name`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM (
      |  SELECT _id AS some_alias
      |  FROM data
      |)
      |WHERE some_alias = ?;
      """.trimMargin(), tempFolder)

    val column = file.findChildrenOfType<SqliteColumnDef>().first()
    file.findChildrenOfType<SqliteBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.sqliteType).isEqualTo(IntermediateType.SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("some_alias")
      assertThat(it.column).isSameAs(column)
    }
  }

  @Test fun `bind args for in statement inherit column name`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |WHERE _id IN ?;
      """.trimMargin(), tempFolder)

    val column = file.findChildrenOfType<SqliteColumnDef>().first()
    file.findChildrenOfType<SqliteBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.sqliteType).isEqualTo(IntermediateType.SqliteType.INTEGER)
      assertThat(it.javaType).isEqualTo(ParameterizedTypeName.get(List::class.asClassName(), List::class.asClassName()))
      assertThat(it.name).isEqualTo("_id")
      assertThat(it.column).isSameAs(column)
    }
  }
}
