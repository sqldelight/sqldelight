package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.core.compiler.model.namedMutators
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MutatorQueryTypeTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `type is generated properly for no result set changes`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS Int NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(_id: kotlin.Int, value: kotlin.collections.List<kotlin.String>?): kotlin.Long {
      |        statement.bindLong(0, _id.toLong())
      |        statement.bindString(1, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        val result = statement.execute()
      |        return result
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `type is generated properly for result set changes in same file`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS Int NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE _id = ?;
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(_id: kotlin.Int, value: kotlin.collections.List<kotlin.String>?): kotlin.Long {
      |        statement.bindLong(0, _id.toLong())
      |        statement.bindString(1, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        val result = statement.execute()
      |        deferAction {
      |            (queryWrapper.dataQueries.selectForId)
      |                    .forEach { it.notifyResultSetChanged() }
      |        }
      |        return result
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `type is generated properly for result set changes in different file`() {
    FixtureCompiler.writeSql("""
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE _id = ?;
      """.trimMargin(), tempFolder, fileName = "OtherData.sq")

    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS Int NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(_id: kotlin.Int, value: kotlin.collections.List<kotlin.String>?): kotlin.Long {
      |        statement.bindLong(0, _id.toLong())
      |        statement.bindString(1, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        val result = statement.execute()
      |        deferAction {
      |            (queryWrapper.otherDataQueries.selectForId)
      |                    .forEach { it.notifyResultSetChanged() }
      |        }
      |        return result
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `type does not include selects with unchanged result sets`() {
    FixtureCompiler.writeSql("""
      |CREATE TABLE other_data (
      |  _id INTEGER NOT NULL PRIMARY KEY
      |);
      |
      |selectForId:
      |SELECT *
      |FROM other_data
      |WHERE _id = ?;
      """.trimMargin(), tempFolder, fileName = "OtherData.sq")

    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER AS Int NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |selectForId:
      |SELECT *
      |FROM other_data
      |WHERE _id = ?;
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(_id: kotlin.Int, value: kotlin.collections.List<kotlin.String>?): kotlin.Long {
      |        statement.bindLong(0, _id.toLong())
      |        statement.bindString(1, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        val result = statement.execute()
      |        return result
      |    }
      |}
      |""".trimMargin())
  }
}