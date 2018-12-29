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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class InsertData {
      |    fun execute(id: kotlin.Long?, value: kotlin.String?) {
      |        val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |        statement.bindLong(1, id)
      |        statement.bindString(2, value)
      |        statement.execute()
      |        notifyQueries(queryWrapper.testQueries.selectData2)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData {
      |    fun execute(id: kotlin.Long) {
      |        val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |DELETE FROM data
      |            |WHERE id = ?
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE, 1)
      |        statement.bindLong(1, id)
      |        statement.execute()
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData {
      |    fun execute(value: kotlin.String?, id: kotlin.Long) {
      |        val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |UPDATE data
      |            |SET value = ?
      |            |WHERE id = ?
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE, 2)
      |        statement.bindString(1, value)
      |        statement.bindLong(2, id)
      |        statement.execute()
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData {
      |    fun execute(value: kotlin.String?, id: kotlin.Long) {
      |        val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |UPDATE data
      |            |SET value = ?
      |            |WHERE id = ?
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE, 2)
      |        statement.bindString(1, value)
      |        statement.bindLong(2, id)
      |        statement.execute()
      |        notifyQueries(queryWrapper.testQueries.selectData2)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData {
      |    fun execute(value: kotlin.String?, id: kotlin.Long) {
      |        val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |UPDATE data
      |            |SET value = ?
      |            |WHERE id = ?
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE, 2)
      |        statement.bindString(1, value)
      |        statement.bindLong(2, id)
      |        statement.execute()
      |        notifyQueries(queryWrapper.testQueries.selectData2)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    Truth.assertThat(generator.type().toString())
        .isEqualTo("""
      |private inner class DeleteData {
      |    fun execute(value: kotlin.String?, id: kotlin.Long) {
      |        val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |UPDATE data
      |            |SET value = ?
      |            |WHERE id = ?
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE, 2)
      |        statement.bindString(1, value)
      |        statement.bindLong(2, id)
      |        statement.execute()
      |    }
      |}
      |""".trimMargin())
  }
}