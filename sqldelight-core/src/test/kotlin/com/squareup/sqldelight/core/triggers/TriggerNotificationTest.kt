package com.squareup.sqldelight.core.triggers

import com.google.common.truth.Truth
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MutatorQueryFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `trigger before insert then insert notifies`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |CREATE TABLE data2 (
      |  value TEXT
      |);
      |
      |CREATE TRIGGER beforeInsertThenInsert
      |BEFORE INSERT ON data
      |BEGIN
      |INSERT INTO data2
      |VALUES (new.value);
      |END;
      |
      |selectData2:
      |SELECT *
      |FROM data2;
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class InsertData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(id: kotlin.Long?, value: kotlin.String?): kotlin.Long {
      |        statement.bindLong(1, id)
      |        statement.bindString(2, value)
      |        val result = statement.execute()
      |        notifyQueries(queryWrapper.testQueries.selectData2)
      |        return result
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `trigger before insert then insert does not notify for delete`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |CREATE TABLE data2 (
      |  value TEXT
      |);
      |
      |CREATE TRIGGER beforeInsertThenInsert
      |BEFORE INSERT ON data
      |BEGIN
      |INSERT INTO data2
      |VALUES (new.value);
      |END;
      |
      |selectData2:
      |SELECT *
      |FROM data2;
      |
      |deleteData:
      |DELETE FROM data
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(id: kotlin.Long): kotlin.Long {
      |        statement.bindLong(1, id)
      |        val result = statement.execute()
      |        return result
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `trigger before insert then insert does not notify for update`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |CREATE TABLE data2 (
      |  value TEXT
      |);
      |
      |CREATE TRIGGER beforeInsertThenInsert
      |BEFORE INSERT ON data
      |BEGIN
      |INSERT INTO data2
      |VALUES (new.value);
      |END;
      |
      |selectData2:
      |SELECT *
      |FROM data2;
      |
      |deleteData:
      |UPDATE data
      |SET value = ?
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(value: kotlin.String?, id: kotlin.Long): kotlin.Long {
      |        statement.bindString(1, value)
      |        statement.bindLong(2, id)
      |        val result = statement.execute()
      |        return result
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `trigger before update then insert notifies`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |CREATE TABLE data2 (
      |  value TEXT
      |);
      |
      |CREATE TRIGGER beforeInsertThenInsert
      |BEFORE UPDATE ON data
      |BEGIN
      |INSERT INTO data2
      |VALUES (new.value);
      |END;
      |
      |selectData2:
      |SELECT *
      |FROM data2;
      |
      |deleteData:
      |UPDATE data
      |SET value = ?
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(value: kotlin.String?, id: kotlin.Long): kotlin.Long {
      |        statement.bindString(1, value)
      |        statement.bindLong(2, id)
      |        val result = statement.execute()
      |        notifyQueries(queryWrapper.testQueries.selectData2)
      |        return result
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `trigger before update columns then insert notifies`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |CREATE TABLE data2 (
      |  value TEXT
      |);
      |
      |CREATE TRIGGER beforeInsertThenInsert
      |BEFORE UPDATE OF value ON data
      |BEGIN
      |INSERT INTO data2
      |VALUES (new.value);
      |END;
      |
      |selectData2:
      |SELECT *
      |FROM data2;
      |
      |deleteData:
      |UPDATE data
      |SET value = ?
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(value: kotlin.String?, id: kotlin.Long): kotlin.Long {
      |        statement.bindString(1, value)
      |        statement.bindLong(2, id)
      |        val result = statement.execute()
      |        notifyQueries(queryWrapper.testQueries.selectData2)
      |        return result
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `trigger before update columns then insert does not notify for different column`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |CREATE TABLE data2 (
      |  value TEXT
      |);
      |
      |CREATE TRIGGER beforeInsertThenInsert
      |BEFORE UPDATE OF id ON data
      |BEGIN
      |INSERT INTO data2
      |VALUES (new.value);
      |END;
      |
      |selectData2:
      |SELECT *
      |FROM data2;
      |
      |deleteData:
      |UPDATE data
      |SET value = ?
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData(private val statement: com.squareup.sqldelight.db.SqlPreparedStatement) {
      |    fun execute(value: kotlin.String?, id: kotlin.Long): kotlin.Long {
      |        statement.bindString(1, value)
      |        statement.bindLong(2, id)
      |        val result = statement.execute()
      |        return result
      |    }
      |}
      |""".trimMargin())
  }
}