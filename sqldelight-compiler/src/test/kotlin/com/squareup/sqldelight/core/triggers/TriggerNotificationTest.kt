package com.squareup.sqldelight.core.triggers

import com.alecstrong.sql.psi.core.DialectPreset
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TriggerNotificationTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `trigger before insert then insert notifies`() {
    val file = FixtureCompiler.parseSql(
      """
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
      """.trimMargin(),
      tempFolder
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public override fun insertData(id: kotlin.Long?, value_: kotlin.String?): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id)
      |    bindString(2, value_)
      |  }
      |  notifyQueries(${mutator.id}) { emit ->
      |    emit(database.testQueries.selectData2)
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test fun `trigger before insert then insert does not notify for delete`() {
    val file = FixtureCompiler.parseSql(
      """
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
      """.trimMargin(),
      tempFolder
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public override fun deleteData(id: kotlin.Long): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |DELETE FROM data
      |  |WHERE id = ?
      |  ""${'"'}.trimMargin(), 1) {
      |    bindLong(1, id)
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test fun `trigger before insert then insert does not notify for update`() {
    val file = FixtureCompiler.parseSql(
      """
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
      """.trimMargin(),
      tempFolder
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public override fun deleteData(value_: kotlin.String?, id: kotlin.Long): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |UPDATE data
      |  |SET value = ?
      |  |WHERE id = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, value_)
      |    bindLong(2, id)
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test fun `trigger before update then insert notifies`() {
    val file = FixtureCompiler.parseSql(
      """
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
      """.trimMargin(),
      tempFolder
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public override fun deleteData(value_: kotlin.String?, id: kotlin.Long): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |UPDATE data
      |  |SET value = ?
      |  |WHERE id = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, value_)
      |    bindLong(2, id)
      |  }
      |  notifyQueries(${mutator.id}) { emit ->
      |    emit(database.testQueries.selectData2)
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test fun `trigger before update columns then insert notifies`() {
    val file = FixtureCompiler.parseSql(
      """
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
      """.trimMargin(),
      tempFolder
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public override fun deleteData(value_: kotlin.String?, id: kotlin.Long): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |UPDATE data
      |  |SET value = ?
      |  |WHERE id = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, value_)
      |    bindLong(2, id)
      |  }
      |  notifyQueries(${mutator.id}) { emit ->
      |    emit(database.testQueries.selectData2)
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test fun `trigger before update columns then insert does not notify for different column`() {
    val file = FixtureCompiler.parseSql(
      """
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
      """.trimMargin(),
      tempFolder
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public override fun deleteData(value_: kotlin.String?, id: kotlin.Long): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |UPDATE data
      |  |SET value = ?
      |  |WHERE id = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, value_)
      |    bindLong(2, id)
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test fun `upsert should account for after update triggers`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |CREATE TABLE data2 (
      |  value TEXT
      |);
      |
      |CREATE TRIGGER afterUpdateThenUpsert
      |AFTER UPDATE ON data
      |BEGIN
      |INSERT INTO data2
      |VALUES (new.value);
      |END;
      |
      |selectData2:
      |SELECT *
      |FROM data2;
      |
      |upsertData:
      |INSERT INTO data (id, value) VALUES (:id, :value)
      |ON CONFLICT (id) DO UPDATE SET value = excluded.value;
      """.trimMargin(),
      tempFolder,
      dialectPreset = DialectPreset.SQLITE_3_24
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public override fun upsertData(id: kotlin.Long?, `value`: kotlin.String?): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data (id, value) VALUES (?, ?)
      |  |ON CONFLICT (id) DO UPDATE SET value = excluded.value
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id)
      |    bindString(2, value)
      |  }
      |  notifyQueries(${mutator.id}) { emit ->
      |    emit(database.testQueries.selectData2)
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test fun `upsert should account for before update triggers`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |CREATE TABLE data2 (
      |  value TEXT
      |);
      |
      |CREATE TRIGGER beforeUpdateThenUpsert
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
      |upsertData:
      |INSERT INTO data (id, value) VALUES (:id, :value)
      |ON CONFLICT (id) DO UPDATE SET value = excluded.value;
      """.trimMargin(),
      tempFolder,
      dialectPreset = DialectPreset.SQLITE_3_24
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public override fun upsertData(id: kotlin.Long?, `value`: kotlin.String?): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data (id, value) VALUES (?, ?)
      |  |ON CONFLICT (id) DO UPDATE SET value = excluded.value
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id)
      |    bindString(2, value)
      |  }
      |  notifyQueries(${mutator.id}) { emit ->
      |    emit(database.testQueries.selectData2)
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test fun `foreign key on delete updates`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE foo (
      |    id INTEGER PRIMARY KEY,
      |    name TEXT
      |);
      |
      |CREATE TABLE bar(
      |    id INTEGER PRIMARY KEY,
      |    name TEXT,
      |    foo_id INTEGER,
      |    FOREIGN KEY (foo_id) REFERENCES foo(id) ON DELETE CASCADE
      |);
      |
      |CREATE TABLE baz(
      |    id INTEGER PRIMARY KEY,
      |    name TEXT,
      |    foo_id INTEGER,
      |    FOREIGN KEY (foo_id) REFERENCES foo(id) ON UPDATE CASCADE
      |);
      |
      |allBars:
      |SELECT * FROM bar;
      |
      |allBaz:
      |SELECT * FROM baz;
      |
      |deleteAllFoos:
      |DELETE FROM foo;
      """.trimMargin(),
      tempFolder,
      dialectPreset = DialectPreset.SQLITE_3_24
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public override fun deleteAllFoos(): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}DELETE FROM foo""${'"'}, 0)
      |  notifyQueries(${mutator.id}) { emit ->
      |    emit(database.testQueries.allBars)
      |  }
      |}
      |""".trimMargin()
    )
  }
}
