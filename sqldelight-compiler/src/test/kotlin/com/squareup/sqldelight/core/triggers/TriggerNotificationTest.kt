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

    Truth.assertThat(generator.function().toString())
        .isEqualTo("""
      |override fun insertData(id: kotlin.Long?, value: kotlin.String?) {
      |    driver.execute(${mutator.id}, ""${'"'}INSERT INTO data VALUES (?1, ?2)""${'"'}, 2) {
      |        bindLong(1, id)
      |        bindString(2, value)
      |    }
      |    notifyQueries(${mutator.id}, {database.testQueries.selectData2})
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

    Truth.assertThat(generator.function().toString())
        .isEqualTo("""
      |override fun deleteData(id: kotlin.Long) {
      |    driver.execute(${mutator.id}, ""${'"'}DELETE FROM data WHERE id = ?1""${'"'}, 1) {
      |        bindLong(1, id)
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

    Truth.assertThat(generator.function().toString())
        .isEqualTo("""
      |override fun deleteData(value: kotlin.String?, id: kotlin.Long) {
      |    driver.execute(${mutator.id}, ""${'"'}UPDATE data SET value = ?1 WHERE id = ?2""${'"'}, 2) {
      |        bindString(1, value)
      |        bindLong(2, id)
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

    Truth.assertThat(generator.function().toString())
        .isEqualTo("""
      |override fun deleteData(value: kotlin.String?, id: kotlin.Long) {
      |    driver.execute(${mutator.id}, ""${'"'}UPDATE data SET value = ?1 WHERE id = ?2""${'"'}, 2) {
      |        bindString(1, value)
      |        bindLong(2, id)
      |    }
      |    notifyQueries(${mutator.id}, {database.testQueries.selectData2})
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

    Truth.assertThat(generator.function().toString())
        .isEqualTo("""
      |override fun deleteData(value: kotlin.String?, id: kotlin.Long) {
      |    driver.execute(${mutator.id}, ""${'"'}UPDATE data SET value = ?1 WHERE id = ?2""${'"'}, 2) {
      |        bindString(1, value)
      |        bindLong(2, id)
      |    }
      |    notifyQueries(${mutator.id}, {database.testQueries.selectData2})
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

    Truth.assertThat(generator.function().toString())
        .isEqualTo("""
      |override fun deleteData(value: kotlin.String?, id: kotlin.Long) {
      |    driver.execute(${mutator.id}, ""${'"'}UPDATE data SET value = ?1 WHERE id = ?2""${'"'}, 2) {
      |        bindString(1, value)
      |        bindLong(2, id)
      |    }
      |}
      |""".trimMargin())
  }
}