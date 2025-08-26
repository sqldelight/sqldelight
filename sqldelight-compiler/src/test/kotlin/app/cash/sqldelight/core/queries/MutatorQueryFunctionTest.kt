package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.TestDialect
import app.cash.sqldelight.core.compiler.ExecuteQueryGenerator
import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.core.dialects.binderCheck
import app.cash.sqldelight.core.dialects.textType
import app.cash.sqldelight.dialects.sqlite_3_24.SqliteDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withUnderscores
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class MutatorQueryFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test
  fun `mutator method generates proper method signature`(dialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  value ${dialect.textType}
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (:customTextValue);
      """.trimMargin(),
      tempFolder,
      dialect = dialect.dialect,
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun insertData(customTextValue: kotlin.String?): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${insert.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        ${dialect.binderCheck}bindString(0, customTextValue)
      |      }
      |  notifyQueries(${insert.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test
  fun `mutator lambda generates proper method signature`(dialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  value ${dialect.textType}
      |);
      |
      |insertData {
      |  INSERT INTO data
      |  VALUES (:customTextValue);
      |}
      """.trimMargin(),
      tempFolder,
      dialect = dialect.dialect,
    )

    val insert = file.namedExecutes.first()
    val generator = ExecuteQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun insertData(customTextValue: kotlin.String?): app.cash.sqldelight.db.QueryResult<kotlin.Long> = transactionWithResult {
      |  driver.execute(${insert.idForIndex(0).withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        ${dialect.binderCheck}bindString(0, customTextValue)
      |      }
      |}.also {
      |  notifyQueries(${insert.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `mutator method generates proper private value`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(),
      tempFolder,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun insertData(id: kotlin.Long?, value_: kotlin.collections.List?): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        var parameterIndex = 0
      |        bindLong(parameterIndex++, id)
      |        bindString(parameterIndex++, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(1_642_410_240) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `delete generates proper type`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value REAL NOT NULL
      |);
      |
      |deleteData:
      |DELETE FROM data;
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun deleteData(): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}DELETE FROM data""${'"'}, 0)
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `mutator method generates proper private value for interface inserts`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES ?;
      """.trimMargin(),
      tempFolder,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun insertData(data_: com.example.Data_): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data (id, value)
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        var parameterIndex = 0
      |        bindLong(parameterIndex++, data_.id)
      |        bindString(parameterIndex++, data_.value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(1_642_410_240) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `mutator method with parameter names`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |updateData:
      |UPDATE data
      |SET value = :newValue
      |WHERE value = :oldValue;
      """.trimMargin(),
      tempFolder,
    )

    val update = file.namedMutators.first()
    val generator = MutatorQueryGenerator(update)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun updateData(newValue: kotlin.collections.List?, oldValue: kotlin.collections.List?): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(null, ""${'"'}
      |      |UPDATE data
      |      |SET value = ?
      |      |WHERE value ${"$"}{ if (oldValue == null) "IS" else "=" } ?
      |      ""${'"'}.trimMargin(), 2) {
      |        var parameterIndex = 0
      |        bindString(parameterIndex++, newValue?.let { data_Adapter.value_Adapter.encode(it) })
      |        bindString(parameterIndex++, oldValue?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(380_313_360) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )

    val nullAsUnknownFile = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |updateData:
      |UPDATE data
      |SET value = :newValue
      |WHERE value = :oldValue;
      """.trimMargin(),
      tempFolder,
      treatNullAsUnknownForEquality = true,
    )

    val nullAsUnknownUpdate = nullAsUnknownFile.namedMutators.first()
    val nullAsUnknownGenerator = MutatorQueryGenerator(nullAsUnknownUpdate)

    assertThat(nullAsUnknownGenerator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun updateData(newValue: kotlin.collections.List?, oldValue: kotlin.collections.List?): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${nullAsUnknownUpdate.id.withUnderscores}, ""${'"'}
      |      |UPDATE data
      |      |SET value = ?
      |      |WHERE value = ?
      |      ""${'"'}.trimMargin(), 2) {
      |        var parameterIndex = 0
      |        bindString(parameterIndex++, newValue?.let { data_Adapter.value_Adapter.encode(it) })
      |        bindString(parameterIndex++, oldValue?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(380_313_360) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `mutator method destructures bind arg into full table`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES ?;
      """.trimMargin(),
      tempFolder,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun insertData(data_: com.example.Data_): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data (id, value)
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        var parameterIndex = 0
      |        bindLong(parameterIndex++, data_.id)
      |        bindString(parameterIndex++, data_.value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(1_642_410_240) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `mutator method destructures bind arg into columns`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List DEFAULT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (id)
      |VALUES ?;
      """.trimMargin(),
      tempFolder,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun insertData(data_: com.example.Data_): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data (id)
      |      |VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(0, data_.id)
      |      }
      |  notifyQueries(1_642_410_240) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `null can be passed in for integer primary keys`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List DEFAULT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (id)
      |VALUES (?);
      """.trimMargin(),
      tempFolder,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun insertData(id: kotlin.Long?): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data (id)
      |      |VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(0, id)
      |      }
      |  notifyQueries(1_642_410_240) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `set parameters for mutator queries`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List DEFAULT NULL
      |);
      |
      |updateData:
      |UPDATE data
      |SET value = ?
      |WHERE id IN ?;
      """.trimMargin(),
      tempFolder,
    )

    val update = file.namedMutators.first()
    val generator = MutatorQueryGenerator(update)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun updateData(value_: kotlin.collections.List?, id: kotlin.collections.Collection<kotlin.Long>): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val idIndexes = createArguments(count = id.size)
      |  val result = driver.execute(null, ""${'"'}
      |      |UPDATE data
      |      |SET value = ?
      |      |WHERE id IN ${"$"}idIndexes
      |      ""${'"'}.trimMargin(), 1 + id.size) {
      |        var parameterIndex = 0
      |        bindString(parameterIndex++, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |        id.forEachIndexed { index, id_ ->
      |          bindLong(parameterIndex + index, id_)
      |        }
      |        parameterIndex += id.size
      |      }
      |  notifyQueries(${update.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `bind parameter inside inner select gets proper type`() {
    val file = FixtureCompiler.parseSql(
      """
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
      """.trimMargin(),
      tempFolder,
    )

    val update = file.namedMutators.first()
    val generator = MutatorQueryGenerator(update)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun updateWithInnerSelect(some_column: kotlin.Long?): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${update.id.withUnderscores}, ""${'"'}
      |      |UPDATE some_table
      |      |SET some_column = (
      |      |  SELECT CASE WHEN ? IS NULL THEN some_column ELSE ? END
      |      |  FROM some_table
      |      |)
      |      ""${'"'}.trimMargin(), 2) {
      |        var parameterIndex = 0
      |        bindLong(parameterIndex++, some_column)
      |        bindLong(parameterIndex++, some_column)
      |      }
      |  notifyQueries(${update.id.withUnderscores}) { emit ->
      |    emit("some_table")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `bind parameters on custom types`() {
    val file = FixtureCompiler.parseSql(
      """
      |import kotlin.String;
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
      """.trimMargin(),
      tempFolder,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)
    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun update(
      |  a: kotlin.String?,
      |  b: kotlin.String?,
      |  c: kotlin.collections.List<kotlin.String>?,
      |  d: kotlin.collections.List<kotlin.String>?,
      |): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |UPDATE paymentHistoryConfig
      |      |SET a = ?,
      |      |    b = ?,
      |      |    c = ?,
      |      |    d = ?
      |      ""${'"'}.trimMargin(), 4) {
      |        var parameterIndex = 0
      |        bindString(parameterIndex++, a)
      |        bindString(parameterIndex++, b)
      |        bindBytes(parameterIndex++, c?.let { paymentHistoryConfigAdapter.cAdapter.encode(it) })
      |        bindBytes(parameterIndex++, d?.let { paymentHistoryConfigAdapter.dAdapter.encode(it) })
      |      }
      |  notifyQueries(-1_559_802_298) { emit ->
      |    emit("paymentHistoryConfig")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `bind parameters in list`() {
    val file = FixtureCompiler.parseSql(
      """
      |import kotlin.String;
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
      |SET (a, b, c, d) = (?, ?, ?, ?);
      """.trimMargin(),
      tempFolder,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)
    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun update(
      |  a: kotlin.String?,
      |  b: kotlin.String?,
      |  c: kotlin.collections.List<kotlin.String>?,
      |  d: kotlin.collections.List<kotlin.String>?,
      |): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |UPDATE paymentHistoryConfig
      |      |SET (a, b, c, d) = (?, ?, ?, ?)
      |      ""${'"'}.trimMargin(), 4) {
      |        var parameterIndex = 0
      |        bindString(parameterIndex++, a)
      |        bindString(parameterIndex++, b)
      |        bindBytes(parameterIndex++, c?.let { paymentHistoryConfigAdapter.cAdapter.encode(it) })
      |        bindBytes(parameterIndex++, d?.let { paymentHistoryConfigAdapter.dAdapter.encode(it) })
      |      }
      |  notifyQueries(-1_559_802_298) { emit ->
      |    emit("paymentHistoryConfig")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `mutator method generates proper method signature for all nullable fields`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE nullableTypes (
      |  val1 TEXT AS kotlin.collections.List<String>,
      |  val2 TEXT
      |);
      |
      |insertNullableType:
      |INSERT INTO nullableTypes
      |VALUES ?;
      """.trimMargin(),
      tempFolder,
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun insertNullableType(nullableTypes: com.example.NullableTypes): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${insert.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO nullableTypes (val1, val2)
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        var parameterIndex = 0
      |        bindString(parameterIndex++, nullableTypes.val1?.let { nullableTypesAdapter.val1Adapter.encode(it) })
      |        bindString(parameterIndex++, nullableTypes.val2)
      |      }
      |  notifyQueries(-871_418_479) { emit ->
      |    emit("nullableTypes")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `subexpressions with shared arguments`() {
    val file = FixtureCompiler.parseSql(
      """
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
      """.trimMargin(),
      tempFolder,
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun save(
      |  id: kotlin.String,
      |  name: kotlin.String,
      |  description: kotlin.String,
      |): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${insert.id.withUnderscores}, ""${'"'}
      |      |INSERT OR REPLACE
      |      |INTO category (rowid, id, name, description)
      |      |VALUES (COALESCE((SELECT rowid FROM category c2 WHERE id = ?), NULL), ?, ?, ?)
      |      ""${'"'}.trimMargin(), 4) {
      |        var parameterIndex = 0
      |        bindString(parameterIndex++, id)
      |        bindString(parameterIndex++, id)
      |        bindString(parameterIndex++, name)
      |        bindString(parameterIndex++, description)
      |      }
      |  notifyQueries(-368_176_582) { emit ->
      |    emit("category")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `reuse encode function result for duplicate types`() {
    val file = FixtureCompiler.parseSql(
      """
      |import java.math.BigDecimal;
      |
      |CREATE TABLE example(
      |  id TEXT PRIMARY KEY NOT NULL,
      |  data TEXT AS BigDecimal
      |);
      |
      |upsert:
      |INSERT INTO example(id, data) VALUES(:id, :data) ON CONFLICT(id) DO UPDATE SET data = :data;
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
      dialect = SqliteDialect(),
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
    |/**
    | * @return The number of rows updated.
    | */
    |public fun upsert(id: kotlin.String, `data`: java.math.BigDecimal?): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
    |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}INSERT INTO example(id, data) VALUES(?, ?) ON CONFLICT(id) DO UPDATE SET data = ?""${'"'}, 3) {
    |        val data__ = data?.let { exampleAdapter.data_Adapter.encode(it) }
    |        var parameterIndex = 0
    |        bindString(parameterIndex++, id)
    |        bindString(parameterIndex++, data__)
    |        bindString(parameterIndex++, data__)
    |      }
    |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
    |    emit("example")
    |  }
      |  return result
    |}
    |
      """.trimMargin(),
    )
  }

  @Test fun `kotlin keywords are mangled in parameters and arguments inside insert stmt`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE annotation (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT
      |);
      |
      |insertAnnotation:
      |INSERT INTO annotation
      |VALUES ?;
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun insertAnnotation(annotation_: com.example.Annotation_): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO annotation (id, name)
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        var parameterIndex = 0
      |        bindLong(parameterIndex++, annotation_.id)
      |        bindString(parameterIndex++, annotation_.name)
      |      }
      |  notifyQueries(1_295_352_605) { emit ->
      |    emit("annotation")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `grouped statement with insert shorthand fails`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE myTable(
      |  id TEXT,
      |  column1 TEXT,
      |  column2 TEXT
      |);
      |
      |upsert {
      |  UPDATE myTable
      |  SET column1 = :column1,
      |      column2 = :column2
      |  WHERE id = :id;
      |  INSERT OR IGNORE INTO myTable VALUES ?;
      |}
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    assertThat(result.errors).containsExactly(
      "Data.sq: (12, 32): Table parameters are not usable in a grouped statement.",
    )
  }

  @Test fun `expressions can infer types from an update clause`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE BasketRowEntity (
      |  subtotal TEXT
      |);
      |
      |update1:
      |UPDATE BasketRowEntity SET subtotal = IIF(TRUE, :subtotal, NULL);
      |
      |update2:
      |UPDATE BasketRowEntity SET subtotal = IIF(TRUE, :subtotal, :subtotal);
      """.trimMargin(),
      tempFolder,
    )

    file.namedMutators.forEach {
      val generator = MutatorQueryGenerator(it)
      assertThat(generator.function().parameters.map { it.toString() }).containsExactly("subtotal: kotlin.String?")
    }
  }

  @Test
  fun `large number of parameters generates efficient incremental indexing`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test_data (
      |  id INTEGER PRIMARY KEY,
      |  param1 TEXT,
      |  param2 TEXT, 
      |  param3 TEXT,
      |  param4 TEXT,
      |  param5 TEXT,
      |  param6 TEXT,
      |  param7 TEXT,
      |  param8 TEXT,
      |  param9 TEXT,
      |  param10 TEXT
      |);
      |
      |insertManyParams:
      |INSERT INTO test_data (param1, param2, param3, param4, param5, param6, param7, param8, param9, param10)
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
      """.trimMargin(),
      tempFolder,
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)
    val generatedFunction = generator.function().toString()

    // Verify the function uses incremental indexing for multiple parameters
    assertThat(generatedFunction).contains("var parameterIndex = 0")
    assertThat(generatedFunction).contains("parameterIndex++")
    
    // Verify it doesn't contain the old O(n²) offset calculations
    assertThat(generatedFunction).doesNotContain("index + 0")
    assertThat(generatedFunction).doesNotContain("index + 1 + 1")
    assertThat(generatedFunction).doesNotContain("0 + 1 + 1")
    
    // Verify each parameter is bound with parameterIndex++
    for (i in 1..10) {
      assertThat(generatedFunction).contains("bindString(parameterIndex++, param$i)")
    }
  }

  @Test
  fun `single parameter does not use incremental indexing`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test_data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT
      |);
      |
      |insertSingleParam:
      |INSERT INTO test_data (value)
      |VALUES (?);
      """.trimMargin(),
      tempFolder,
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)
    val generatedFunction = generator.function().toString()

    // For single parameter, should use simple hardcoded indexing
    assertThat(generatedFunction).doesNotContain("var parameterIndex = 0")
    assertThat(generatedFunction).doesNotContain("parameterIndex++")
    assertThat(generatedFunction).contains("bindString(0, value_)")
  }

  @Test
  fun `array parameters with regular parameters generate efficient incremental indexing`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT,
      |  category TEXT
      |);
      |
      |updateWithArrays:
      |UPDATE data 
      |SET value = ?
      |WHERE id IN :ids 
      |AND category = ?;
      """.trimMargin(),
      tempFolder,
    )

    val update = file.namedMutators.first()
    val generator = MutatorQueryGenerator(update)
    val generatedFunction = generator.function().toString()

    // Verify incremental indexing is used (multiple parameters including array)
    assertThat(generatedFunction).contains("var parameterIndex = 0")
    assertThat(generatedFunction).contains("parameterIndex++")
    
    // Verify array parameter indexing
    assertThat(generatedFunction).contains("bindLong(parameterIndex + index, ids_)")
    assertThat(generatedFunction).contains("parameterIndex += ids.size")
    
    // Verify regular parameter after array uses incremental indexing
    assertThat(generatedFunction).contains("bindString(parameterIndex++, category)")
    
    // Verify no complex offset calculations
    assertThat(generatedFunction).doesNotContain("1 + ids.size")
    assertThat(generatedFunction).doesNotContain("0 + 1 + ids.size")
  }

  @Test fun `multiple array parameters generate efficient incremental indexing`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT,
      |  category TEXT
      |);
      |
      |selectMultipleArrays:
      |SELECT * FROM data 
      |WHERE id IN :ids 
      |AND value IN :values
      |AND category = ?;
      """.trimMargin(),
      tempFolder,
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)
    val generatedQueryType = generator.querySubtype().toString()

    // Verify incremental indexing is used
    assertThat(generatedQueryType).contains("var parameterIndex = 0")
    
    // Verify first array parameter
    assertThat(generatedQueryType).contains("bindLong(parameterIndex + index, ids_)")
    assertThat(generatedQueryType).contains("parameterIndex += ids.size")
    
    // Verify second array parameter uses updated parameterIndex
    assertThat(generatedQueryType).contains("bindString(parameterIndex + index, values_)")
    assertThat(generatedQueryType).contains("parameterIndex += values.size")
    
    // Verify regular parameter after arrays
    assertThat(generatedQueryType).contains("bindString(parameterIndex++, category)")
    
    // The total argument count calculation still includes the old format for arrays
    // This is expected as it's calculated before the incremental binding logic
    assertThat(generatedQueryType).contains("1 + ids.size + values.size")
  }
}
