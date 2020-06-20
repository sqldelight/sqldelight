package com.squareup.sqldelight.core.queries

import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.DialectPreset.HSQL
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.core.dialects.textType
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class MutatorQueryFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test
  fun `mutator method generates proper method signature`(dialect: DialectPreset) {
    assumeTrue(dialect !in listOf(HSQL))
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value ${dialect.textType}
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (:customTextValue);
      """.trimMargin(), tempFolder, dialectPreset = dialect)

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertData(customTextValue: kotlin.String?) {
      |  driver.execute(${insert.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?)
      |  ""${'"'}.trimMargin(), 1) {
      |    bindString(1, customTextValue)
      |  }
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
      |override fun insertData(id: kotlin.Long?, value: kotlin.collections.List?) {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id)
      |    bindString(2, value?.let { database.dataAdapter.valueAdapter.encode(it) })
      |  }
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
      |override fun deleteData() {
      |  driver.execute(${mutator.id}, ""${'"'}DELETE FROM data""${'"'}, 0)
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
      |override fun insertData(data: com.example.Data) {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, data.id)
      |    bindString(2, data.value?.let { database.dataAdapter.valueAdapter.encode(it) })
      |  }
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
      |override fun updateData(newValue: kotlin.collections.List?, oldValue: kotlin.collections.List?) {
      |  driver.execute(null, ""${'"'}
      |  |UPDATE data
      |  |SET value = ?
      |  |WHERE value ${"$"}{ if (oldValue == null) "IS" else "=" } ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, newValue?.let { database.dataAdapter.valueAdapter.encode(it) })
      |    bindString(2, oldValue?.let { database.dataAdapter.valueAdapter.encode(it) })
      |  }
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
      |override fun insertData(data: com.example.Data) {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, data.id)
      |    bindString(2, data.value?.let { database.dataAdapter.valueAdapter.encode(it) })
      |  }
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
      |override fun insertData(data: com.example.Data) {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data (id)
      |  |VALUES (?)
      |  ""${'"'}.trimMargin(), 1) {
      |    bindLong(1, data.id)
      |  }
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
      |override fun insertData(id: kotlin.Long?) {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data (id)
      |  |VALUES (?)
      |  ""${'"'}.trimMargin(), 1) {
      |    bindLong(1, id)
      |  }
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
      |override fun updateData(value: kotlin.collections.List?, id: kotlin.collections.Collection<kotlin.Long>) {
      |  val idIndexes = createArguments(count = id.size)
      |  driver.execute(null, ""${'"'}
      |  |UPDATE data
      |  |SET value = ?
      |  |WHERE id IN ${"$"}idIndexes
      |  ""${'"'}.trimMargin(), 1 + id.size) {
      |    bindString(1, value?.let { database.dataAdapter.valueAdapter.encode(it) })
      |    id.forEachIndexed { index, id_ ->
      |        bindLong(index + 2, id_)
      |        }
      |  }
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
      |override fun updateWithInnerSelect(some_column: kotlin.Long?) {
      |  driver.execute(${update.id}, ""${'"'}
      |  |UPDATE some_table
      |  |SET some_column = (
      |  |  SELECT CASE WHEN ? IS NULL THEN some_column ELSE ? END
      |  |  FROM some_table
      |  |)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, some_column)
      |    bindLong(2, some_column)
      |  }
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
      |override fun update(
      |  a: kotlin.String?,
      |  b: kotlin.String?,
      |  c: kotlin.collections.List<kotlin.String>?,
      |  d: kotlin.collections.List<kotlin.String>?
      |) {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |UPDATE paymentHistoryConfig
      |  |SET a = ?,
      |  |    b = ?,
      |  |    c = ?,
      |  |    d = ?
      |  ""${'"'}.trimMargin(), 4) {
      |    bindString(1, a)
      |    bindString(2, b)
      |    bindBytes(3, c?.let { database.paymentHistoryConfigAdapter.cAdapter.encode(it) })
      |    bindBytes(4, d?.let { database.paymentHistoryConfigAdapter.dAdapter.encode(it) })
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun `mutator method generates proper method signature for all nullable fields`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE nullableTypes (
      |  val1 TEXT AS kotlin.collections.List<String>,
      |  val2 TEXT
      |);
      |
      |insertNullableType:
      |INSERT INTO nullableTypes
      |VALUES ?;
      """.trimMargin(), tempFolder)

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertNullableType(nullableTypes: com.example.NullableTypes) {
      |  driver.execute(${insert.id}, ""${'"'}
      |  |INSERT INTO nullableTypes
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, nullableTypes.val1?.let { database.nullableTypesAdapter.val1Adapter.encode(it) })
      |    bindString(2, nullableTypes.val2)
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun `subexpressions with shared arguments`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE category(
      |  id TEXT NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  description TEXT NOT NULL
      |);
      |
      |save:
      |INSERT OR REPLACE
      |INTO category (rowid, id, name, description)
      |VALUES (COALESCE((SELECT rowid FROM category c2 WHERE id = ?1), NULL), ?1, ?2, ?3);
      """.trimMargin(), tempFolder)

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun save(
      |  id: kotlin.String,
      |  name: kotlin.String,
      |  description: kotlin.String
      |) {
      |  driver.execute(${insert.id}, ""${'"'}
      |  |INSERT OR REPLACE
      |  |INTO category (rowid, id, name, description)
      |  |VALUES (COALESCE((SELECT rowid FROM category c2 WHERE id = ?), NULL), ?, ?, ?)
      |  ""${'"'}.trimMargin(), 4) {
      |    bindString(1, id)
      |    bindString(2, id)
      |    bindString(3, name)
      |    bindString(4, description)
      |  }
      |}
      |""".trimMargin())
  }
}
