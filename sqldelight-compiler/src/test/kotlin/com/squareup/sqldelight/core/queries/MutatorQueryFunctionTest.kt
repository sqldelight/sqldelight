package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MutatorQueryFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `mutator method generates proper method signature`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder)

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(id: kotlin.Long?, value: kotlin.collections.List?) {
      |    val statement = database.prepareStatement(${insert.id}, ""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?1, ?2)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |    statement.bindLong(1, id)
      |    statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `mutator method generates proper private value`() {
     val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder)

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(id: kotlin.Long?, value: kotlin.collections.List?) {
      |    val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?1, ?2)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |    statement.bindLong(1, id)
      |    statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `delete generates proper type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value REAL NOT NULL
      |);
      |
      |deleteData:
      |DELETE FROM data;
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |fun deleteData() {
      |    val statement = database.prepareStatement(${mutator.id}, ""${'"'}DELETE FROM data""${'"'}, com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE, 0)
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `mutator method generates proper private value for interface inserts`() {
     val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES ?;
      """.trimMargin(), tempFolder)

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(data: com.example.Data) {
      |    val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |    statement.bindLong(1, data.id)
      |    statement.bindString(2, if (data.value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(data.value!!))
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `mutator method with parameter names`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |updateData:
      |UPDATE data
      |SET value = :newValue
      |WHERE value = :oldValue;
      """.trimMargin(), tempFolder)

    val update = file.namedMutators.first()
    val generator = MutatorQueryGenerator(update)

    assertThat(generator.function().toString()).isEqualTo("""
      |fun updateData(newValue: kotlin.collections.List?, oldValue: kotlin.collections.List?) {
      |    val statement = database.prepareStatement(${update.id}, ""${'"'}
      |            |UPDATE data
      |            |SET value = ?1
      |            |WHERE value ${"$"}{ if (oldValue == null) "IS" else "=" } ?2
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE, 2)
      |    statement.bindString(1, if (newValue == null) null else queryWrapper.dataAdapter.valueAdapter.encode(newValue))
      |    statement.bindString(2, if (oldValue == null) null else queryWrapper.dataAdapter.valueAdapter.encode(oldValue))
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `mutator method destructures bind arg into full table`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES ?;
      """.trimMargin(), tempFolder)

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(data: com.example.Data) {
      |    val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |    statement.bindLong(1, data.id)
      |    statement.bindString(2, if (data.value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(data.value!!))
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `mutator method destructures bind arg into columns`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List DEFAULT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (id)
      |VALUES ?;
      """.trimMargin(), tempFolder)

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(data: com.example.Data) {
      |    val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |INSERT INTO data (id)
      |            |VALUES (?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 1)
      |    statement.bindLong(1, data.id)
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `null can be passed in for integer primary keys`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List DEFAULT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (id)
      |VALUES (?);
      """.trimMargin(), tempFolder)

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(id: kotlin.Long?) {
      |    val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |INSERT INTO data (id)
      |            |VALUES (?1)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 1)
      |    statement.bindLong(1, id)
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `set parameters for mutator queries`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List DEFAULT NULL
      |);
      |
      |updateData:
      |UPDATE data
      |SET value = ?
      |WHERE id IN ?;
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.function().toString()).isEqualTo("""
      |fun updateData(value: kotlin.collections.List?, id: kotlin.collections.Collection<kotlin.Long>) {
      |    val idIndexes = createArguments(count = id.size, offset = 3)
      |    val statement = database.prepareStatement(null, ""${'"'}
      |            |UPDATE data
      |            |SET value = ?1
      |            |WHERE id IN ${"$"}idIndexes
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE, 1 + id.size)
      |    statement.bindString(1, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |    id.forEachIndexed { index, id ->
      |            statement.bindLong(index + 3, id)
      |            }
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `bind parameter inside inner select gets proper type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE some_table (
      |  some_column INTEGER NOT NULL
      |);
      |
      |updateWithInnerSelect:
      |UPDATE some_table
      |SET some_column = (
      |  SELECT CASE WHEN ?1 IS NULL THEN some_column ELSE ?1 END
      |  FROM some_table
      |);
      """.trimMargin(), tempFolder)

    val update = file.namedMutators.first()
    val generator = MutatorQueryGenerator(update)

    assertThat(generator.function().toString()).isEqualTo("""
      |fun updateWithInnerSelect(some_column: kotlin.Long?) {
      |    val statement = database.prepareStatement(${update.id}, ""${'"'}
      |            |UPDATE some_table
      |            |SET some_column = (
      |            |  SELECT CASE WHEN ?1 IS NULL THEN some_column ELSE ?1 END
      |            |  FROM some_table
      |            |)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE, 1)
      |    statement.bindLong(1, some_column)
      |    statement.execute()
      |}
      |""".trimMargin())
  }

  @Test fun `bind parameters on custom types`() {
    val file = FixtureCompiler.parseSql("""
      |import kotlin.collections.List;
      |
      |CREATE TABLE paymentHistoryConfig (
      |  a TEXT DEFAULT NULL,
      |  b TEXT DEFAULT NULL,
      |  c BLOB AS List<String> DEFAULT NULL,
      |  d BLOB AS List<String> DEFAULT NULL
      |);
      |
      |update:
      |UPDATE paymentHistoryConfig
      |SET a = ?,
      |    b = ?,
      |    c = ?,
      |    d = ?;
      """.trimMargin(), tempFolder)

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)
    assertThat(generator.function().toString()).isEqualTo("""
      |fun update(
      |    a: kotlin.String?,
      |    b: kotlin.String?,
      |    c: kotlin.collections.List<kotlin.String>?,
      |    d: kotlin.collections.List<kotlin.String>?
      |) {
      |    val statement = database.prepareStatement(${mutator.id}, ""${'"'}
      |            |UPDATE paymentHistoryConfig
      |            |SET a = ?1,
      |            |    b = ?2,
      |            |    c = ?3,
      |            |    d = ?4
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE, 4)
      |    statement.bindString(1, a)
      |    statement.bindString(2, b)
      |    statement.bindBytes(3, if (c == null) null else queryWrapper.paymentHistoryConfigAdapter.cAdapter.encode(c))
      |    statement.bindBytes(4, if (d == null) null else queryWrapper.paymentHistoryConfigAdapter.dAdapter.encode(d))
      |    statement.execute()
      |}
      |""".trimMargin())
  }
}
