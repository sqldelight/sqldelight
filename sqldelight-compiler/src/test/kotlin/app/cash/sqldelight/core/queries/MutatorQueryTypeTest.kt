package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.TestDialect
import app.cash.sqldelight.core.compiler.ExecuteQueryGenerator
import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
import app.cash.sqldelight.core.dialects.intType
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withUnderscores
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class MutatorQueryTypeTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `type is generated properly for no result set changes`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<kotlin.String>
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
      |public fun insertData(id: kotlin.Int?, value_: kotlin.collections.List<kotlin.String>?) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        bindLong(0, id?.let { data_Adapter.idAdapter.encode(it) })
      |        bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(1_642_410_240) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `type is generated properly with adapter and questionMark`() {
    val file = FixtureCompiler.parseSql(
      """
    |import foo.S;
    |
    |CREATE TABLE data (
    |  id INTEGER AS kotlin.Int PRIMARY KEY,
    |  value TEXT AS S
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
    |public fun insertData(data_: com.example.Data_) {
    |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
    |      |INSERT INTO data (id, value)
    |      |VALUES (?, ?)
    |      ""${'"'}.trimMargin(), 2) {
    |        bindLong(0, data_Adapter.idAdapter.encode(data_.id))
    |        bindString(1, data_.value_?.let { data_Adapter.value_Adapter.encode(it) })
    |      }
    |  notifyQueries(1_642_410_240) { emit ->
    |    emit("data")
    |  }
    |}
    |
      """.trimMargin(),
    )
  }

  @Test fun `bind argument order is consistent with sql`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE item(
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  packageName TEXT NOT NULL,
      |  className TEXT NOT NULL,
      |  deprecated INTEGER AS kotlin.Boolean NOT NULL DEFAULT 0,
      |  link TEXT NOT NULL,
      |
      |  UNIQUE (packageName, className)
      |);
      |
      |updateItem:
      |UPDATE item
      |SET deprecated = ?3,
      |    link = ?4
      |WHERE packageName = ?1
      |  AND className = ?2
      |;
      """.trimMargin(),
      tempFolder,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun updateItem(
      |  packageName: kotlin.String,
      |  className: kotlin.String,
      |  deprecated: kotlin.Boolean,
      |  link: kotlin.String,
      |) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |UPDATE item
      |      |SET deprecated = ?,
      |      |    link = ?
      |      |WHERE packageName = ?
      |      |  AND className = ?
      |      ""${'"'}.trimMargin(), 4) {
      |        bindBoolean(0, deprecated)
      |        bindString(1, link)
      |        bindString(2, packageName)
      |        bindString(3, className)
      |      }
      |  notifyQueries(380_480_121) { emit ->
      |    emit("item")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `type is generated properly for result set changes in same file`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<kotlin.String>
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertData(id: kotlin.Int?, value_: kotlin.collections.List<kotlin.String>?) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        bindLong(0, id?.let { data_Adapter.idAdapter.encode(it) })
      |        bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `type is generated properly for result set changes in different file`() {
    FixtureCompiler.writeSql(
      """
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder,
      fileName = "OtherData.sq",
    )

    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<kotlin.String>
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertData(id: kotlin.Int?, value_: kotlin.collections.List<kotlin.String>?) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        bindLong(0, id?.let { data_Adapter.idAdapter.encode(it) })
      |        bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `type does not include selects with unchanged result sets`() {
    FixtureCompiler.writeSql(
      """
      |CREATE TABLE other_data (
      |  id INTEGER NOT NULL PRIMARY KEY
      |);
      |
      |selectForId:
      |SELECT *
      |FROM other_data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder,
      fileName = "OtherData.sq",
    )

    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.Int NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<kotlin.String>
      |);
      |
      |selectForId:
      |SELECT *
      |FROM other_data
      |WHERE id = ?;
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertData(id: kotlin.Int?, value_: kotlin.collections.List<kotlin.String>?) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        bindLong(0, id?.let { data_Adapter.idAdapter.encode(it) })
      |        bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(208_179_736) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `null can be passed for integer primary keys`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<kotlin.String>
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertData(id: kotlin.Int?, value_: kotlin.collections.List<kotlin.String>?) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        bindLong(0, id?.let { data_Adapter.idAdapter.encode(it) })
      |        bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `mutator query has inner select`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = 1;
      |
      |deleteData:
      |DELETE FROM data
      |WHERE id = 1
      |AND value IN (
      |  SELECT data.value
      |  FROM data
      |  INNER JOIN data AS data2
      |  ON data.id = data2.id
      |);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun deleteData() {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |DELETE FROM data
      |      |WHERE id = 1
      |      |AND value IN (
      |      |  SELECT data.value
      |      |  FROM data
      |      |  INNER JOIN data AS data2
      |      |  ON data.id = data2.id
      |      |)
      |      ""${'"'}.trimMargin(), 0)
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `blob binds fine`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value BLOB NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (value)
      |VALUES (?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertData(value_: kotlin.ByteArray) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindBytes(0, value_)
      |      }
      |  notifyQueries(208_179_736) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `real binds fine`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value REAL NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (value)
      |VALUES (?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertData(value_: kotlin.Double) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindDouble(0, value_)
      |      }
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `types bind fine in HSQL`(dialect: TestDialect) {
    assumeTrue(dialect == TestDialect.HSQL)

    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  boolean0 BOOLEAN NOT NULL,
      |  boolean1 BOOLEAN,
      |  boolean2 BOOLEAN AS kotlin.String NOT NULL,
      |  boolean3 BOOLEAN AS kotlin.String,
      |  tinyint0 TINYINT NOT NULL,
      |  tinyint1 TINYINT,
      |  tinyint2 TINYINT AS kotlin.String NOT NULL,
      |  tinyint3 TINYINT AS kotlin.String,
      |  smallint0 SMALLINT NOT NULL,
      |  smallint1 SMALLINT,
      |  smallint2 SMALLINT AS kotlin.String NOT NULL,
      |  smallint3 SMALLINT AS kotlin.String,
      |  int0 ${dialect.intType} NOT NULL,
      |  int1 ${dialect.intType},
      |  int2 ${dialect.intType} AS kotlin.String NOT NULL,
      |  int3 ${dialect.intType} AS kotlin.String,
      |  bigint0 BIGINT NOT NULL,
      |  bigint1 BIGINT,
      |  bigint2 BIGINT AS kotlin.String NOT NULL,
      |  bigint3 BIGINT AS kotlin.String
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
      dialect.dialect,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertData(
      |  boolean0: kotlin.Boolean,
      |  boolean1: kotlin.Boolean?,
      |  boolean2: kotlin.String,
      |  boolean3: kotlin.String?,
      |  tinyint0: kotlin.Byte,
      |  tinyint1: kotlin.Byte?,
      |  tinyint2: kotlin.String,
      |  tinyint3: kotlin.String?,
      |  smallint0: kotlin.Short,
      |  smallint1: kotlin.Short?,
      |  smallint2: kotlin.String,
      |  smallint3: kotlin.String?,
      |  int0: kotlin.Int,
      |  int1: kotlin.Int?,
      |  int2: kotlin.String,
      |  int3: kotlin.String?,
      |  bigint0: kotlin.Long,
      |  bigint1: kotlin.Long?,
      |  bigint2: kotlin.String,
      |  bigint3: kotlin.String?,
      |) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |      ""${'"'}.trimMargin(), 20) {
      |        check(this is ${dialect.dialect.runtimeTypes.preparedStatementType})
      |        bindLong(0, if (boolean0) 1L else 0L)
      |        bindLong(1, boolean1?.let { if (it) 1L else 0L })
      |        bindLong(2, if (data_Adapter.boolean2Adapter.encode(boolean2)) 1L else 0L)
      |        bindLong(3, boolean3?.let { if (data_Adapter.boolean3Adapter.encode(it)) 1L else 0L })
      |        bindLong(4, tinyint0.toLong())
      |        bindLong(5, tinyint1?.let { it.toLong() })
      |        bindLong(6, data_Adapter.tinyint2Adapter.encode(tinyint2).toLong())
      |        bindLong(7, tinyint3?.let { data_Adapter.tinyint3Adapter.encode(it).toLong() })
      |        bindLong(8, smallint0.toLong())
      |        bindLong(9, smallint1?.let { it.toLong() })
      |        bindLong(10, data_Adapter.smallint2Adapter.encode(smallint2).toLong())
      |        bindLong(11, smallint3?.let { data_Adapter.smallint3Adapter.encode(it).toLong() })
      |        bindLong(12, int0.toLong())
      |        bindLong(13, int1?.let { it.toLong() })
      |        bindLong(14, data_Adapter.int2Adapter.encode(int2).toLong())
      |        bindLong(15, int3?.let { data_Adapter.int3Adapter.encode(it).toLong() })
      |        bindLong(16, bigint0)
      |        bindLong(17, bigint1)
      |        bindLong(18, data_Adapter.bigint2Adapter.encode(bigint2))
      |        bindLong(19, bigint3?.let { data_Adapter.bigint3Adapter.encode(it) })
      |      }
      |  notifyQueries(208_179_736) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `types bind fine in MySQL`(dialect: TestDialect) {
    assumeTrue(dialect == TestDialect.MYSQL)

    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  boolean0 BOOLEAN NOT NULL,
      |  boolean1 BOOLEAN,
      |  boolean2 BOOLEAN AS kotlin.String NOT NULL,
      |  boolean3 BOOLEAN AS kotlin.String,
      |  bit0 BIT NOT NULL,
      |  bit1 BIT,
      |  bit2 BIT AS kotlin.String NOT NULL,
      |  bit3 BIT AS kotlin.String,
      |  tinyint0 TINYINT NOT NULL,
      |  tinyint1 TINYINT,
      |  tinyint2 TINYINT AS kotlin.String NOT NULL,
      |  tinyint3 TINYINT AS kotlin.String,
      |  smallint0 SMALLINT NOT NULL,
      |  smallint1 SMALLINT,
      |  smallint2 SMALLINT AS kotlin.String NOT NULL,
      |  smallint3 SMALLINT AS kotlin.String,
      |  bigint0 BIGINT NOT NULL,
      |  bigint1 BIGINT,
      |  bigint2 BIGINT AS kotlin.String NOT NULL,
      |  bigint3 BIGINT AS kotlin.String
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
      dialect.dialect,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertData(
      |  boolean0: kotlin.Boolean,
      |  boolean1: kotlin.Boolean?,
      |  boolean2: kotlin.String,
      |  boolean3: kotlin.String?,
      |  bit0: kotlin.Boolean,
      |  bit1: kotlin.Boolean?,
      |  bit2: kotlin.String,
      |  bit3: kotlin.String?,
      |  tinyint0: kotlin.Byte,
      |  tinyint1: kotlin.Byte?,
      |  tinyint2: kotlin.String,
      |  tinyint3: kotlin.String?,
      |  smallint0: kotlin.Short,
      |  smallint1: kotlin.Short?,
      |  smallint2: kotlin.String,
      |  smallint3: kotlin.String?,
      |  bigint0: kotlin.Long,
      |  bigint1: kotlin.Long?,
      |  bigint2: kotlin.String,
      |  bigint3: kotlin.String?,
      |) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |      ""${'"'}.trimMargin(), 20) {
      |        check(this is ${dialect.dialect.runtimeTypes.preparedStatementType})
      |        bindLong(0, if (boolean0) 1L else 0L)
      |        bindLong(1, boolean1?.let { if (it) 1L else 0L })
      |        bindLong(2, if (data_Adapter.boolean2Adapter.encode(boolean2)) 1L else 0L)
      |        bindLong(3, boolean3?.let { if (data_Adapter.boolean3Adapter.encode(it)) 1L else 0L })
      |        bindLong(4, if (bit0) 1L else 0L)
      |        bindLong(5, bit1?.let { if (it) 1L else 0L })
      |        bindLong(6, if (data_Adapter.bit2Adapter.encode(bit2)) 1L else 0L)
      |        bindLong(7, bit3?.let { if (data_Adapter.bit3Adapter.encode(it)) 1L else 0L })
      |        bindLong(8, tinyint0.toLong())
      |        bindLong(9, tinyint1?.let { it.toLong() })
      |        bindLong(10, data_Adapter.tinyint2Adapter.encode(tinyint2).toLong())
      |        bindLong(11, tinyint3?.let { data_Adapter.tinyint3Adapter.encode(it).toLong() })
      |        bindLong(12, smallint0.toLong())
      |        bindLong(13, smallint1?.let { it.toLong() })
      |        bindLong(14, data_Adapter.smallint2Adapter.encode(smallint2).toLong())
      |        bindLong(15, smallint3?.let { data_Adapter.smallint3Adapter.encode(it).toLong() })
      |        bindLong(16, bigint0)
      |        bindLong(17, bigint1)
      |        bindLong(18, data_Adapter.bigint2Adapter.encode(bigint2))
      |        bindLong(19, bigint3?.let { data_Adapter.bigint3Adapter.encode(it) })
      |      }
      |  notifyQueries(208_179_736) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `types bind fine in PostgreSQL`(dialect: TestDialect) {
    assumeTrue(dialect == TestDialect.POSTGRESQL)

    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  smallint0 SMALLINT NOT NULL,
      |  smallint1 SMALLINT,
      |  smallint2 SMALLINT AS kotlin.String NOT NULL,
      |  smallint3 SMALLINT AS kotlin.String,
      |  int0 ${dialect.intType} NOT NULL,
      |  int1 ${dialect.intType},
      |  int2 ${dialect.intType} AS kotlin.String NOT NULL,
      |  int3 ${dialect.intType} AS kotlin.String,
      |  bigint0 BIGINT NOT NULL,
      |  bigint1 BIGINT,
      |  bigint2 BIGINT AS kotlin.String NOT NULL,
      |  bigint3 BIGINT AS kotlin.String
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
      dialect.dialect,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertData(
      |  smallint0: kotlin.Short,
      |  smallint1: kotlin.Short?,
      |  smallint2: kotlin.String,
      |  smallint3: kotlin.String?,
      |  int0: kotlin.Int,
      |  int1: kotlin.Int?,
      |  int2: kotlin.String,
      |  int3: kotlin.String?,
      |  bigint0: kotlin.Long,
      |  bigint1: kotlin.Long?,
      |  bigint2: kotlin.String,
      |  bigint3: kotlin.String?,
      |) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |      ""${'"'}.trimMargin(), 12) {
      |        check(this is ${dialect.dialect.runtimeTypes.preparedStatementType})
      |        bindShort(0, smallint0)
      |        bindShort(1, smallint1)
      |        bindShort(2, data_Adapter.smallint2Adapter.encode(smallint2))
      |        bindShort(3, smallint3?.let { data_Adapter.smallint3Adapter.encode(it) })
      |        bindInt(4, int0)
      |        bindInt(5, int1)
      |        bindInt(6, data_Adapter.int2Adapter.encode(int2))
      |        bindInt(7, int3?.let { data_Adapter.int3Adapter.encode(it) })
      |        bindLong(8, bigint0)
      |        bindLong(9, bigint1)
      |        bindLong(10, data_Adapter.bigint2Adapter.encode(bigint2))
      |        bindLong(11, bigint3?.let { data_Adapter.bigint3Adapter.encode(it) })
      |      }
      |  notifyQueries(208_179_736) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `insert with triggers and virtual tables is fine`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE item(
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  packageName TEXT NOT NULL,
      |  className TEXT NOT NULL,
      |  deprecated INTEGER AS kotlin.Boolean NOT NULL DEFAULT 0,
      |  link TEXT NOT NULL,
      |
      |  UNIQUE (packageName, className)
      |);
      |
      |CREATE VIRTUAL TABLE item_index USING fts4(content TEXT);
      |
      |insertItem:
      |INSERT OR FAIL INTO item(packageName, className, deprecated, link) VALUES (?, ?, ?, ?)
      |;
      |
      |queryTerm:
      |SELECT item.*
      |FROM item_index
      |JOIN item ON (docid = item.id)
      |WHERE content LIKE '%' || ? || '%' ESCAPE '\'
      |;
      |
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertItem(
      |  packageName: kotlin.String,
      |  className: kotlin.String,
      |  deprecated: kotlin.Boolean,
      |  link: kotlin.String,
      |) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}INSERT OR FAIL INTO item(packageName, className, deprecated, link) VALUES (?, ?, ?, ?)""${'"'}, 4) {
      |        bindString(0, packageName)
      |        bindString(1, className)
      |        bindBoolean(2, deprecated)
      |        bindString(3, link)
      |      }
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("item")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `insert with triggers and fts5 virtual tables is fine`() {
    val file = FixtureCompiler.parseSql(
      """
    |CREATE TABLE item(
    |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    |  packageName TEXT NOT NULL,
    |  className TEXT NOT NULL,
    |  deprecated INTEGER AS Boolean NOT NULL DEFAULT 0,
    |  link TEXT NOT NULL,
    |
    |  UNIQUE (packageName, className)
    |);
    |
    |CREATE VIRTUAL TABLE item_index USING fts5(content TEXT, prefix='2 3 4 5 6 7', content_rowid=id);
    |
    |insertItem:
    |INSERT OR FAIL INTO item_index(content) VALUES (?);
    |
    |queryTerm:
    |SELECT item.*
    |FROM item_index
    |JOIN item ON (docid = item.id)
    |WHERE content MATCH '"one ' || ? || '" * ';
    |
    |
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
    |public fun insertItem(content: kotlin.String?) {
    |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}INSERT OR FAIL INTO item_index(content) VALUES (?)""${'"'}, 1) {
    |        bindString(0, content)
    |      }
    |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
    |    emit("item_index")
    |  }
    |}
    |
      """.trimMargin(),
    )
  }

  @Test fun `update on a view with a trigger`() {
    val file = FixtureCompiler.parseSql(
      """
    |CREATE TABLE foo (
    |  id INTEGER NOT NULL PRIMARY KEY,
    |  name TEXT NOT NULL,
    |  selected INTEGER AS kotlin.Boolean NOT NULL
    |);
    |
    |CREATE TABLE bar (
    |  id INTEGER NOT NULL PRIMARY KEY,
    |  short_name TEXT NOT NULL,
    |  full_name TEXT NOT NULL,
    |  selected INTEGER AS kotlin.Boolean NOT NULL
    |);
    |
    |CREATE VIEW foobar AS
    |SELECT id, name, selected FROM foo
    |UNION
    |SELECT id, short_name AS name, selected FROM bar;
    |
    |CREATE TRIGGER foobar_update_added
    |INSTEAD OF UPDATE OF selected ON foobar
    |BEGIN
    |  UPDATE foo SET selected = new.selected WHERE id = new.id;
    |  UPDATE bar SET selected = new.selected WHERE id = new.id;
    |END;
    |
    |updateFoobarSelected:
    |UPDATE OR IGNORE foobar SET selected = ? WHERE id = ?;
    |
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
    |public fun updateFoobarSelected(selected: kotlin.Boolean, id: kotlin.Long) {
    |  driver.execute(1_531_606_598, ""${'"'}UPDATE OR IGNORE foobar SET selected = ? WHERE id = ?""${'"'}, 2) {
    |        bindBoolean(0, selected)
    |        bindLong(1, id)
    |      }
    |  notifyQueries(1_531_606_598) { emit ->
    |    emit("bar")
    |    emit("foo")
    |  }
    |}
    |
      """.trimMargin(),
    )
  }

  @Test fun `delete using named list parameter twice is fine`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  other INTEGER AS Int NOT NULL
      |);
      |
      |delete {
      |  DELETE FROM data WHERE id IN :ids;
      |  DELETE FROM data WHERE other IN :ids;
      |}
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
    )

    val mutator = file.namedExecutes.first()
    val generator = ExecuteQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun delete(ids: kotlin.collections.Collection<Int>) {
      |  transaction {
      |    val idsIndexes = createArguments(count = ids.size)
      |    driver.execute(null, ""${'"'}DELETE FROM data WHERE id IN ${'$'}idsIndexes""${'"'}, ids.size) {
      |          ids.forEachIndexed { index, ids_ ->
      |            bindLong(index, data_Adapter.idAdapter.encode(ids_))
      |          }
      |        }
      |    driver.execute(null, ""${'"'}DELETE FROM data WHERE other IN ${'$'}idsIndexes""${'"'}, ids.size) {
      |          ids.forEachIndexed { index, ids_ ->
      |            bindLong(index, data_Adapter.idAdapter.encode(ids_))
      |          }
      |        }
      |  }
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }
}
