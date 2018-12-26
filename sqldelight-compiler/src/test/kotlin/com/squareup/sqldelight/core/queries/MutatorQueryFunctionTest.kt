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

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(id: kotlin.Long?, value: kotlin.collections.List?) {
      |    insertData.execute(id, value)
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

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    fun execute(id: kotlin.Long?, value: kotlin.collections.List?) {
      |        val statement = database.getConnection().prepareStatement(${mutator.id}, ""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |        statement.bindLong(1, id)
      |        statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        statement.execute()
      |    }
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

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class DeleteData {
      |    fun execute() {
      |        val statement = database.getConnection().prepareStatement(${mutator.id}, "DELETE FROM data", com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE, 0)
      |        statement.execute()
      |    }
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

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    fun execute(id: kotlin.Long, value: kotlin.collections.List?) {
      |        val statement = database.getConnection().prepareStatement(${mutator.id}, ""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |        statement.bindLong(1, id)
      |        statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        statement.execute()
      |    }
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

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.function().toString()).isEqualTo("""
      |fun updateData(newValue: kotlin.collections.List?, oldValue: kotlin.collections.List?) {
      |    updateData.execute(newValue, oldValue)
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

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(data: com.example.Data) {
      |    insertData.execute(data.id, data.value)
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

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(data: com.example.Data) {
      |    insertData.execute(data.id)
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

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(id: kotlin.Long?) {
      |    insertData.execute(id)
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
      |    val statement = database.getConnection().prepareStatement(null, ""${'"'}
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

    val generator = MutatorQueryGenerator(file.namedMutators.first())
    assertThat(generator.function().toString()).isEqualTo("""
      |fun updateWithInnerSelect(some_column: kotlin.Long?) {
      |    updateWithInnerSelect.execute(some_column)
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

    val generator = MutatorQueryGenerator(file.namedMutators.first())
    assertThat(generator.function().toString()).isEqualTo("""
      |fun update(
      |    a: kotlin.String?,
      |    b: kotlin.String?,
      |    c: kotlin.collections.List<kotlin.String>?,
      |    d: kotlin.collections.List<kotlin.String>?
      |) {
      |    update.execute(a, b, c, d)
      |}
      |""".trimMargin())
  }
}
