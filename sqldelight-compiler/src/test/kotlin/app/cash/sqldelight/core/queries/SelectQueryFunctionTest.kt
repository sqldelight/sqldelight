package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.TestDialect
import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.core.dialects.intType
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class SelectQueryFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `query function with default result type generates properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectForId(id: kotlin.Long): app.cash.sqldelight.Query<com.example.Data_> = selectForId(id) { id_, value_ ->
      |  com.example.Data_(
      |    id_,
      |    value_
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `infer type for between`() {
    val file = FixtureCompiler.parseSql(
      """
      |import com.example.LocalDateTime;
      |
      |CREATE TABLE data (
      |  channelId TEXT NOT NULL,
      |  startTime INTEGER AS LocalDateTime NOT NULL,
      |  endTime INTEGER AS LocalDateTime NOT NULL
      |);
      |
      |selectByChannelId:
      |SELECT *
      |FROM data
      |WHERE channelId =?
      |AND (
      |  startTime BETWEEN :from AND :to
      |  OR
      |  endTime BETWEEN :from AND :to
      |  OR
      |  :from BETWEEN startTime AND endTime
      |  OR
      |  :to BETWEEN startTime AND endTime
      |);
      |""".trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectByChannelId(
      |  channelId: kotlin.String,
      |  from: com.example.LocalDateTime,
      |  to: com.example.LocalDateTime,
      |): app.cash.sqldelight.Query<com.example.Data_> = selectByChannelId(channelId, from, to) { channelId_, startTime, endTime ->
      |  com.example.Data_(
      |    channelId_,
      |    startTime,
      |    endTime
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `query bind args appear in the correct order`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |select:
      |SELECT *
      |FROM data
      |WHERE id = ?2
      |AND value = ?1;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun select(value_: kotlin.String, id: kotlin.Long): app.cash.sqldelight.Query<com.example.Data_> = select(value_, id) { id_, value__ ->
      |  com.example.Data_(
      |    id_,
      |    value__
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `query function with custom result type generates properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
    |public fun <T : kotlin.Any> selectForId(id: kotlin.Long, mapper: (id: kotlin.Long, value_: kotlin.String) -> T): app.cash.sqldelight.Query<T> = SelectForIdQuery(id) { cursor ->
    |  mapper(
    |    cursor.getLong(0)!!,
    |    cursor.getString(1)!!
    |  )
    |}
    |
      """.trimMargin()
    )
  }

  @Test fun `custom result type query function uses adapter`() {
    val file = FixtureCompiler.parseSql(
      """
      |import kotlin.collections.List;
      |
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT AS List NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectForId(id: kotlin.Long, mapper: (id: kotlin.Long, value_: kotlin.collections.List) -> T): app.cash.sqldelight.Query<T> = SelectForIdQuery(id) { cursor ->
      |  mapper(
      |    cursor.getLong(0)!!,
      |    data_Adapter.value_Adapter.decode(cursor.getString(1)!!)
      |  )
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `multiple values types are folded into proper result type`() {
    val file = FixtureCompiler.parseSql(
      """
      |selectValues:
      |VALUES (1), ('sup');
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).contains(
      """
      |public fun selectValues(): app.cash.sqldelight.Query<kotlin.String>
      """.trimMargin()
    )
  }

  @Test fun `query with no parameters doesn't subclass Query`() {
    val file = FixtureCompiler.parseSql(
      """
      |import kotlin.collections.List;
      |
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT AS List NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectForId(mapper: (id: kotlin.Long, value_: kotlin.collections.List) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(${query.id}, arrayOf("data"), driver, "Test.sq", "selectForId", ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |  mapper(
      |    cursor.getLong(0)!!,
      |    data_Adapter.value_Adapter.decode(cursor.getString(1)!!)
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `integer primary key is always exposed as non-null`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectData(): app.cash.sqldelight.Query<kotlin.Long> = app.cash.sqldelight.Query(${query.id}, arrayOf("data"), driver, "Test.sq", "selectData", ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |  cursor.getLong(0)!!
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `bind parameter used in IN expression explodes into multiple query args`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id IN :good AND id NOT IN :bad;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  public val good: kotlin.collections.Collection<kotlin.Long>,
      |  public val bad: kotlin.collections.Collection<kotlin.Long>,
      |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
      |) : app.cash.sqldelight.Query<T>(mapper) {
      |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
      |    driver.addListener(listener, arrayOf("data"))
      |  }
      |
      |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
      |    driver.removeListener(listener, arrayOf("data"))
      |  }
      |
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor {
      |    val goodIndexes = createArguments(count = good.size)
      |    val badIndexes = createArguments(count = bad.size)
      |    return driver.executeQuery(null, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id IN ${"$"}goodIndexes AND id NOT IN ${"$"}badIndexes
      |    ""${'"'}.trimMargin(), good.size + bad.size) {
      |      good.forEachIndexed { index, good_ ->
      |          bindLong(index + 1, good_)
      |          }
      |      bad.forEachIndexed { index, bad_ ->
      |          bindLong(index + good.size + 1, bad_)
      |          }
      |    }
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `limit and offset bind expressions gets proper types`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  some_column INTEGER NOT NULL,
      |  some_column2 INTEGER NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |WHERE EXISTS (SELECT * FROM data LIMIT :minimum OFFSET :offset)
      |LIMIT :minimum;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun someSelect(minimum: kotlin.Long, offset: kotlin.Long): app.cash.sqldelight.Query<com.example.Data_> = someSelect(minimum, offset) { some_column, some_column2 ->
      |  com.example.Data_(
      |    some_column,
      |    some_column2
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `named bind arg can be reused`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE person (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  first_name TEXT NOT NULL,
      |  last_name TEXT NOT NULL
      |);
      |
      |equivalentNamesNamed:
      |SELECT *
      |FROM person
      |WHERE first_name = :name AND last_name = :name;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class EquivalentNamesNamedQuery<out T : kotlin.Any>(
      |  public val name: kotlin.String,
      |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
      |) : app.cash.sqldelight.Query<T>(mapper) {
      |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
      |    driver.addListener(listener, arrayOf("person"))
      |  }
      |
      |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
      |    driver.removeListener(listener, arrayOf("person"))
      |  }
      |
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${query.id}, ""${'"'}
      |  |SELECT *
      |  |FROM person
      |  |WHERE first_name = ? AND last_name = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, name)
      |    bindString(2, name)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:equivalentNamesNamed"
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `real is exposed properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  value REAL NOT NULL
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectData(): app.cash.sqldelight.Query<kotlin.Double> = app.cash.sqldelight.Query(${query.id}, arrayOf("data"), driver, "Test.sq", "selectData", ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |  cursor.getDouble(0)!!
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `blob is exposed properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  value BLOB NOT NULL
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectData(): app.cash.sqldelight.Query<kotlin.ByteArray> = app.cash.sqldelight.Query(${query.id}, arrayOf("data"), driver, "Test.sq", "selectData", ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |  cursor.getBytes(0)!!
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `null is exposed properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |selectData:
      |SELECT NULL;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectData(mapper: (expr: java.lang.Void?) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(${query.id}, emptyArray(), driver, "Test.sq", "selectData", "SELECT NULL") { cursor ->
      |  mapper(
      |    null
      |  )
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `types are exposed properly in HSQL`(dialect: TestDialect) {
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
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(),
      tempFolder, dialect = dialect.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectData(mapper: (
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
      |) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(${query.id}, arrayOf("data"), driver, "Test.sq", "selectData", ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |  check(cursor is ${dialect.dialect.cursorType})
      |  mapper(
      |    cursor.getLong(0)!! == 1L,
      |    cursor.getLong(1)?.let { it == 1L },
      |    data_Adapter.boolean2Adapter.decode(cursor.getLong(2)!! == 1L),
      |    cursor.getLong(3)?.let { data_Adapter.boolean3Adapter.decode(it == 1L) },
      |    cursor.getLong(4)!!.toByte(),
      |    cursor.getLong(5)?.let { it.toByte() },
      |    data_Adapter.tinyint2Adapter.decode(cursor.getLong(6)!!.toByte()),
      |    cursor.getLong(7)?.let { data_Adapter.tinyint3Adapter.decode(it.toByte()) },
      |    cursor.getLong(8)!!.toShort(),
      |    cursor.getLong(9)?.let { it.toShort() },
      |    data_Adapter.smallint2Adapter.decode(cursor.getLong(10)!!.toShort()),
      |    cursor.getLong(11)?.let { data_Adapter.smallint3Adapter.decode(it.toShort()) },
      |    cursor.getLong(12)!!.toInt(),
      |    cursor.getLong(13)?.let { it.toInt() },
      |    data_Adapter.int2Adapter.decode(cursor.getLong(14)!!.toInt()),
      |    cursor.getLong(15)?.let { data_Adapter.int3Adapter.decode(it.toInt()) },
      |    cursor.getLong(16)!!,
      |    cursor.getLong(17),
      |    data_Adapter.bigint2Adapter.decode(cursor.getLong(18)!!),
      |    cursor.getLong(19)?.let { data_Adapter.bigint3Adapter.decode(it) }
      |  )
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `types are exposed properly in MySQL`(dialect: TestDialect) {
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
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(),
      tempFolder, dialect = dialect.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectData(mapper: (
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
      |  int0: kotlin.Int,
      |  int1: kotlin.Int?,
      |  int2: kotlin.String,
      |  int3: kotlin.String?,
      |  bigint0: kotlin.Long,
      |  bigint1: kotlin.Long?,
      |  bigint2: kotlin.String,
      |  bigint3: kotlin.String?,
      |) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(${query.id}, arrayOf("data"), driver, "Test.sq", "selectData", ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |  check(cursor is ${dialect.dialect.cursorType})
      |  mapper(
      |    cursor.getLong(0)!! == 1L,
      |    cursor.getLong(1)?.let { it == 1L },
      |    data_Adapter.boolean2Adapter.decode(cursor.getLong(2)!! == 1L),
      |    cursor.getLong(3)?.let { data_Adapter.boolean3Adapter.decode(it == 1L) },
      |    cursor.getLong(4)!! == 1L,
      |    cursor.getLong(5)?.let { it == 1L },
      |    data_Adapter.bit2Adapter.decode(cursor.getLong(6)!! == 1L),
      |    cursor.getLong(7)?.let { data_Adapter.bit3Adapter.decode(it == 1L) },
      |    cursor.getLong(8)!!.toByte(),
      |    cursor.getLong(9)?.let { it.toByte() },
      |    data_Adapter.tinyint2Adapter.decode(cursor.getLong(10)!!.toByte()),
      |    cursor.getLong(11)?.let { data_Adapter.tinyint3Adapter.decode(it.toByte()) },
      |    cursor.getLong(12)!!.toShort(),
      |    cursor.getLong(13)?.let { it.toShort() },
      |    data_Adapter.smallint2Adapter.decode(cursor.getLong(14)!!.toShort()),
      |    cursor.getLong(15)?.let { data_Adapter.smallint3Adapter.decode(it.toShort()) },
      |    cursor.getLong(16)!!.toInt(),
      |    cursor.getLong(17)?.let { it.toInt() },
      |    data_Adapter.int2Adapter.decode(cursor.getLong(18)!!.toInt()),
      |    cursor.getLong(19)?.let { data_Adapter.int3Adapter.decode(it.toInt()) },
      |    cursor.getLong(20)!!,
      |    cursor.getLong(21),
      |    data_Adapter.bigint2Adapter.decode(cursor.getLong(22)!!),
      |    cursor.getLong(23)?.let { data_Adapter.bigint3Adapter.decode(it) }
      |  )
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `types are exposed properly in PostgreSQL`(dialect: TestDialect) {
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
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(),
      tempFolder, dialect = dialect.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectData(mapper: (
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
      |) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(${query.id}, arrayOf("data"), driver, "Test.sq", "selectData", ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |  check(cursor is ${dialect.dialect.cursorType})
      |  mapper(
      |    cursor.getLong(0)!!.toShort(),
      |    cursor.getLong(1)?.let { it.toShort() },
      |    data_Adapter.smallint2Adapter.decode(cursor.getLong(2)!!.toShort()),
      |    cursor.getLong(3)?.let { data_Adapter.smallint3Adapter.decode(it.toShort()) },
      |    cursor.getLong(4)!!.toInt(),
      |    cursor.getLong(5)?.let { it.toInt() },
      |    data_Adapter.int2Adapter.decode(cursor.getLong(6)!!.toInt()),
      |    cursor.getLong(7)?.let { data_Adapter.int3Adapter.decode(it.toInt()) },
      |    cursor.getLong(8)!!,
      |    cursor.getLong(9),
      |    data_Adapter.bigint2Adapter.decode(cursor.getLong(10)!!),
      |    cursor.getLong(11)?.let { data_Adapter.bigint3Adapter.decode(it) }
      |  )
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `query returns custom query type`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  value INTEGER,
      |  value2 INTEGER
      |);
      |
      |selectData:
      |SELECT coalesce(value, value2), value, value2
      |FROM data;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectData(): app.cash.sqldelight.Query<com.example.SelectData> = selectData { coalesce, value_, value2 ->
      |  com.example.SelectData(
      |    coalesce,
      |    value_,
      |    value2
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `optional parameter with type inferred from case expression`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE a (
      |  name TEXT NOT NULL
      |);
      |
      |broken:
      |SELECT a.name
      |FROM a
      |WHERE
      |  :input IS NULL
      |  OR :input =
      |    CASE name
      |      WHEN 'a' THEN 'b'
      |      ELSE name
      |    END;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun broken(input: kotlin.String?): app.cash.sqldelight.Query<kotlin.String> = BrokenQuery(input) { cursor ->
      |  cursor.getString(0)!!
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `projection with more columns than there are runtime Function types`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE bigTable (
      |  val1 INTEGER,
      |  val2 INTEGER,
      |  val3 INTEGER,
      |  val4 INTEGER,
      |  val5 INTEGER,
      |  val6 INTEGER,
      |  val7 INTEGER,
      |  val8 INTEGER,
      |  val9 INTEGER,
      |  val10 INTEGER,
      |  val11 INTEGER,
      |  val12 INTEGER,
      |  val13 INTEGER,
      |  val14 INTEGER,
      |  val15 INTEGER,
      |  val16 INTEGER,
      |  val17 INTEGER,
      |  val18 INTEGER,
      |  val19 INTEGER,
      |  val20 INTEGER,
      |  val21 INTEGER,
      |  val22 INTEGER,
      |  val23 INTEGER,
      |  val24 INTEGER,
      |  val25 INTEGER,
      |  val26 INTEGER,
      |  val27 INTEGER,
      |  val28 INTEGER,
      |  val29 INTEGER,
      |  val30 INTEGER
      |);
      |
      |select:
      |SELECT *
      |FROM bigTable;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> select(mapper: (
      |  val1: kotlin.Long?,
      |  val2: kotlin.Long?,
      |  val3: kotlin.Long?,
      |  val4: kotlin.Long?,
      |  val5: kotlin.Long?,
      |  val6: kotlin.Long?,
      |  val7: kotlin.Long?,
      |  val8: kotlin.Long?,
      |  val9: kotlin.Long?,
      |  val10: kotlin.Long?,
      |  val11: kotlin.Long?,
      |  val12: kotlin.Long?,
      |  val13: kotlin.Long?,
      |  val14: kotlin.Long?,
      |  val15: kotlin.Long?,
      |  val16: kotlin.Long?,
      |  val17: kotlin.Long?,
      |  val18: kotlin.Long?,
      |  val19: kotlin.Long?,
      |  val20: kotlin.Long?,
      |  val21: kotlin.Long?,
      |  val22: kotlin.Long?,
      |  val23: kotlin.Long?,
      |  val24: kotlin.Long?,
      |  val25: kotlin.Long?,
      |  val26: kotlin.Long?,
      |  val27: kotlin.Long?,
      |  val28: kotlin.Long?,
      |  val29: kotlin.Long?,
      |  val30: kotlin.Long?,
      |) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(-1626977671, arrayOf("bigTable"), driver, "Test.sq", "select", ""${'"'}
      ||SELECT *
      ||FROM bigTable
      |""${'"'}.trimMargin()) { cursor ->
      |  mapper(
      |    cursor.getLong(0),
      |    cursor.getLong(1),
      |    cursor.getLong(2),
      |    cursor.getLong(3),
      |    cursor.getLong(4),
      |    cursor.getLong(5),
      |    cursor.getLong(6),
      |    cursor.getLong(7),
      |    cursor.getLong(8),
      |    cursor.getLong(9),
      |    cursor.getLong(10),
      |    cursor.getLong(11),
      |    cursor.getLong(12),
      |    cursor.getLong(13),
      |    cursor.getLong(14),
      |    cursor.getLong(15),
      |    cursor.getLong(16),
      |    cursor.getLong(17),
      |    cursor.getLong(18),
      |    cursor.getLong(19),
      |    cursor.getLong(20),
      |    cursor.getLong(21),
      |    cursor.getLong(22),
      |    cursor.getLong(23),
      |    cursor.getLong(24),
      |    cursor.getLong(25),
      |    cursor.getLong(26),
      |    cursor.getLong(27),
      |    cursor.getLong(28),
      |    cursor.getLong(29)
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `match expression on column in FTS table`() {
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
      |queryTerm:
      |SELECT item.*
      |FROM item_index
      |JOIN item ON (docid = item.id)
      |WHERE content MATCH ?1
      |ORDER BY
      |  -- deprecated classes are always last
      |  deprecated ASC,
      |  CASE
      |    -- exact match
      |    WHEN className LIKE ?1 ESCAPE '\' THEN 1
      |    -- prefix match with no nested type
      |    WHEN className LIKE ?1 || '%' ESCAPE '\' AND instr(className, '.') = 0 THEN 2
      |    -- exact match on nested type
      |    WHEN className LIKE '%.' || ?1 ESCAPE '\' THEN 3
      |    -- prefix match (allowing nested types)
      |    WHEN className LIKE ?1 || '%' ESCAPE '\' THEN 4
      |    -- prefix match on nested type
      |    WHEN className LIKE '%.' || ?1 || '%' ESCAPE '\' THEN 5
      |    -- infix match
      |    ELSE 6
      |  END ASC,
      |  -- prefer "closer" matches based on length
      |  length(className) ASC,
      |  -- alphabetize to eliminate any remaining non-determinism
      |  packageName ASC,
      |  className ASC
      |LIMIT 50
      |;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> queryTerm(content: kotlin.String, mapper: (
      |  id: kotlin.Long,
      |  packageName: kotlin.String,
      |  className: kotlin.String,
      |  deprecated: kotlin.Boolean,
      |  link: kotlin.String,
      |) -> T): app.cash.sqldelight.Query<T> = QueryTermQuery(content) { cursor ->
      |  mapper(
      |    cursor.getLong(0)!!,
      |    cursor.getString(1)!!,
      |    cursor.getString(2)!!,
      |    itemAdapter.deprecatedAdapter.decode(cursor.getLong(3)!!),
      |    cursor.getString(4)!!
      |  )
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `match expression on FTS table name`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE place(
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  name TEXT NOT NULL,
      |  shortName TEXT NOT NULL,
      |  category TEXT NOT NULL
      |);
      |
      |CREATE VIRTUAL TABLE place_fts USING fts4(
      |  name TEXT NOT NULL,
      |  shortName TEXT NOT NULL
      |);
      |
      |selectPlace:
      |SELECT place.*
      |FROM place_fts
      |JOIN place ON place_fts.rowid = place.id
      |WHERE place_fts MATCH ?1
      |ORDER BY rank(matchinfo(place_fts)), place.name;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectPlace(place_fts: kotlin.String, mapper: (
      |  id: kotlin.Long,
      |  name: kotlin.String,
      |  shortName: kotlin.String,
      |  category: kotlin.String,
      |) -> T): app.cash.sqldelight.Query<T> = SelectPlaceQuery(place_fts) { cursor ->
      |  mapper(
      |    cursor.getLong(0)!!,
      |    cursor.getString(1)!!,
      |    cursor.getString(2)!!,
      |    cursor.getString(3)!!
      |  )
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `adapted column in inner query exposed in projection`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE testA (
      |  id TEXT NOT NULL PRIMARY KEY,
      |  status TEXT AS Test.Status,
      |  attr TEXT
      |);
      |
      |someSelect:
      |SELECT *
      |FROM (
      |  SELECT *, 1 AS ordering
      |  FROM testA
      |  WHERE testA.attr IS NOT NULL
      |
      |  UNION
      |
      |  SELECT *, 2 AS ordering
      |         FROM testA
      |  WHERE testA.attr IS NULL
      |);
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> someSelect(mapper: (
      |  id: kotlin.String,
      |  status: Test.Status?,
      |  attr: kotlin.String?,
      |  ordering: kotlin.Long,
      |) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(-602300915, arrayOf("testA"), driver, "Test.sq", "someSelect", ""${'"'}
      ||SELECT *
      ||FROM (
      ||  SELECT *, 1 AS ordering
      ||  FROM testA
      ||  WHERE testA.attr IS NOT NULL
      ||
      ||  UNION
      ||
      ||  SELECT *, 2 AS ordering
      ||         FROM testA
      ||  WHERE testA.attr IS NULL
      ||)
      |""${'"'}.trimMargin()) { cursor ->
      |  mapper(
      |    cursor.getString(0)!!,
      |    cursor.getString(1)?.let { testAAdapter.statusAdapter.decode(it) },
      |    cursor.getString(2),
      |    cursor.getLong(3)!!
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `adapted column in foreign table exposed properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE testA (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  parent_id INTEGER NOT NULL,
      |  child_id INTEGER NOT NULL,
      |  FOREIGN KEY (parent_id) REFERENCES testB(_id),
      |  FOREIGN KEY (child_id) REFERENCES testB(_id)
      |);
      |
      |CREATE TABLE testB(
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  category TEXT AS java.util.List NOT NULL,
      |  type TEXT AS java.util.List NOT NULL,
      |  name TEXT NOT NULL
      |);
      |
      |exact_match:
      |SELECT *
      |FROM testA
      |JOIN testB AS parentJoined ON parent_id = parentJoined._id
      |JOIN testB AS childJoined ON child_id = childJoined._id
      |WHERE parent_id = ? AND child_id = ?;
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> exact_match(
      |  parent_id: kotlin.Long,
      |  child_id: kotlin.Long,
      |  mapper: (
      |    _id: kotlin.Long,
      |    parent_id: kotlin.Long,
      |    child_id: kotlin.Long,
      |    _id_: kotlin.Long,
      |    category: java.util.List,
      |    type: java.util.List,
      |    name: kotlin.String,
      |    _id__: kotlin.Long,
      |    category_: java.util.List,
      |    type_: java.util.List,
      |    name_: kotlin.String,
      |  ) -> T,
      |): app.cash.sqldelight.Query<T> = Exact_matchQuery(parent_id, child_id) { cursor ->
      |  mapper(
      |    cursor.getLong(0)!!,
      |    cursor.getLong(1)!!,
      |    cursor.getLong(2)!!,
      |    cursor.getLong(3)!!,
      |    testBAdapter.categoryAdapter.decode(cursor.getString(4)!!),
      |    testBAdapter.typeAdapter.decode(cursor.getString(5)!!),
      |    cursor.getString(6)!!,
      |    cursor.getLong(7)!!,
      |    testBAdapter.categoryAdapter.decode(cursor.getString(8)!!),
      |    testBAdapter.typeAdapter.decode(cursor.getString(9)!!),
      |    cursor.getString(10)!!
      |  )
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun `is not null has correct type`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test (
      |  stuff INTEGER
      |);
      |
      |someSelect:
      |SELECT stuff
      |FROM test
      |WHERE stuff IS NOT NULL;
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun someSelect(): app.cash.sqldelight.Query<kotlin.Long> = app.cash.sqldelight.Query(-602300915, arrayOf("test"), driver, "Test.sq", "someSelect", ""${'"'}
      ||SELECT stuff
      ||FROM test
      ||WHERE stuff IS NOT NULL
      |""${'"'}.trimMargin()) { cursor ->
      |  cursor.getLong(0)!!
      |}
      |""".trimMargin()
    )
  }

  @Test fun `division has correct type`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test (
      |  stuff INTEGER NOT NULL
      |);
      |
      |someSelect:
      |SELECT SUM(stuff) / 3.0
      |FROM test;
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> someSelect(mapper: (expr: kotlin.Double?) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(-602300915, arrayOf("test"), driver, "Test.sq", "someSelect", ""${'"'}
      ||SELECT SUM(stuff) / 3.0
      ||FROM test
      |""${'"'}.trimMargin()) { cursor ->
      |  mapper(
      |    cursor.getDouble(0)
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `type inference on boolean`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE exit (
      |  wingsuit INTEGER AS kotlin.Boolean NOT NULL
      |);
      |
      |queryOne:
      |SELECT * FROM exit WHERE wingsuit = :wingsuit AND :wingsuit = 1;
      |
      |queryTwo:
      |SELECT * FROM exit WHERE :wingsuit = 1 AND wingsuit = :wingsuit;
      """.trimMargin(),
      tempFolder
    )

    val queryOne = file.namedQueries.first()
    val generatorOne = SelectQueryGenerator(queryOne)
    val queryTwo = file.namedQueries.first()
    val generatorTwo = SelectQueryGenerator(queryTwo)

    val param = ParameterSpec.builder("wingsuit", Boolean::class.asTypeName()).build()
    assertThat(generatorOne.defaultResultTypeFunction().parameters).containsExactly(param)
    assertThat(generatorTwo.defaultResultTypeFunction().parameters).containsExactly(param)
  }

  @Test fun `instr second parameter is a string`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE `models` (
      |  `model_id` int(11) NOT NULL AUTO_INCREMENT,
      |  `model_descriptor_id` int(11) NOT NULL,
      |  `model_description` varchar(8) NOT NULL,
      |  PRIMARY KEY (`model_id`)
      |) DEFAULT CHARSET=latin1;
      |
      |searchDescription:
      |SELECT model_id, model_description FROM models WHERE INSTR(model_description, ?) > 0;
      """.trimMargin(),
      tempFolder, dialect = TestDialect.MYSQL.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> searchDescription(`value`: kotlin.String, mapper: (model_id: kotlin.Int, model_description: kotlin.String) -> T): app.cash.sqldelight.Query<T> = SearchDescriptionQuery(value) { cursor ->
      |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
      |  mapper(
      |    cursor.getLong(0)!!.toInt(),
      |    cursor.getString(1)!!
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `type inference on instr`() {
    val file = FixtureCompiler.parseSql(
      """
      |selectIfNull:
      |SELECT 1, 2
      |WHERE IFNULL(:param, 1) > 0;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    val param = ParameterSpec.builder(
      "param_",
      Long::class.asTypeName()
        .copy(nullable = true)
    )
      .build()
    assertThat(generator.defaultResultTypeFunction().parameters).containsExactly(param)
  }

  @Test fun `annotations on a type returned in a function`() {
    val file = FixtureCompiler.parseSql(
      """
      |import java.lang.Deprecated;
      |import kotlin.String;
      |
      |CREATE TABLE category (
      |  accent_color TEXT AS @Deprecated String,
      |  other_thing TEXT AS @Deprecated String
      |);
      |
      |selectAll:
      |SELECT * FROM category;
      """.trimMargin(),
      tempFolder, dialect = TestDialect.MYSQL.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectAll(mapper: (accent_color: kotlin.String?, other_thing: kotlin.String?) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(${query.id}, arrayOf("category"), driver, "Test.sq", "selectAll", "SELECT * FROM category") { cursor ->
      |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
      |  mapper(
      |    cursor.getString(0)?.let { categoryAdapter.accent_colorAdapter.decode(it) },
      |    cursor.getString(1)?.let { categoryAdapter.other_thingAdapter.decode(it) }
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `union of two views uses the view type`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE sup(
      |  value TEXT
      |);
      |
      |CREATE VIEW supView AS
      |SELECT value AS value1, value AS value2
      |FROM sup;
      |
      |unioned:
      |SELECT *
      |FROM supView
      |
      |UNION ALL
      |
      |SELECT *
      |FROM supView;
      """.trimMargin(),
      tempFolder, dialect = TestDialect.MYSQL.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun unioned(): app.cash.sqldelight.Query<com.example.SupView> = unioned { value1, value2 ->
      |  com.example.SupView(
      |    value1,
      |    value2
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `union of two tables uses the function type`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE sup(
      |  value TEXT
      |);
      |
      |CREATE TABLE sup2(
      |  value TEXT
      |);
      |
      |unioned:
      |SELECT *
      |FROM sup
      |
      |UNION ALL
      |
      |SELECT *
      |FROM sup2;
      """.trimMargin(),
      tempFolder, dialect = TestDialect.MYSQL.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun unioned(): app.cash.sqldelight.Query<com.example.Unioned> = unioned { value_ ->
      |  com.example.Unioned(
      |    value_
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `union of a table with itself uses the table type`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE sup(
      |  value TEXT
      |);
      |
      |unioned:
      |SELECT *
      |FROM sup
      |
      |UNION ALL
      |
      |SELECT *
      |FROM sup;
      """.trimMargin(),
      tempFolder, dialect = TestDialect.MYSQL.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun unioned(): app.cash.sqldelight.Query<com.example.Sup> = unioned { value_ ->
      |  com.example.Sup(
      |    value_
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `view that unions two types uses the view type for star projections`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE TestTable1(
      | id TEXT,
      | name TEXT
      |);
      |
      |CREATE TABLE TestTable2(
      | id TEXT,
      | name TEXT
      |);
      |
      |CREATE VIEW TestView AS
      |SELECT
      |  id,
      |  name
      |FROM TestTable1
      |UNION ALL
      |SELECT
      |  id,
      |  name
      |FROM TestTable2;
      |
      |findAll:
      |SELECT * FROM TestView;
      """.trimMargin(),
      tempFolder, dialect = TestDialect.MYSQL.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun findAll(): app.cash.sqldelight.Query<com.example.TestView> = findAll { id, name ->
      |  com.example.TestView(
      |    id,
      |    name
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `if return type computes correctly`() {
    val file = FixtureCompiler.parseSql(
      """
      |selectIf:
      |SELECT IF(1 == 1, 'yes', 'no');
      """.trimMargin(),
      tempFolder, dialect = TestDialect.MYSQL.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectIf(): app.cash.sqldelight.Query<kotlin.String> = app.cash.sqldelight.Query(${query.id}, emptyArray(), driver, "Test.sq", "selectIf", "SELECT IF(1 == 1, 'yes', 'no')") { cursor ->
      |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
      |  cursor.getString(0)!!
      |}
      |""".trimMargin()
    )
  }

  @Test fun `query function with integer named parameter template computes correctly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE Player(
      |  id TEXT PRIMARY KEY,
      |  username TEXT NOT NULL,
      |  email TEXT NOT NULL
      |);
      |
      |select_default_with_query:
      |SELECT *
      |FROM Player
      |WHERE username LIKE ('%' || ?1 || '%') OR email LIKE ('%' || ?1 || '%');
      """.trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun select_default_with_query(value_: kotlin.String): app.cash.sqldelight.Query<com.example.Player> = select_default_with_query(value_) { id, username, email ->
      |  com.example.Player(
      |    id,
      |    username,
      |    email
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `query function with limit and offset computes correctly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE StorePermissions (
      |  userId VARCHAR(32) NOT NULL,
      |  organizationId INTEGER NOT NULL,
      |  storeId INTEGER,
      |  roleId INTEGER NOT NULL,
      |  PRIMARY KEY (userId, organizationId, storeId),
      |  FOREIGN KEY (userId) REFERENCES Users(id),
      |  INDEX(userId),
      |  FOREIGN KEY (organizationId) REFERENCES Organizations(id),
      |  INDEX(organizationId),
      |  FOREIGN KEY (storeId) REFERENCES Stores(id),
      |  INDEX(storeId),
      |  FOREIGN KEY (roleId) REFERENCES StoreRoles(id)
      |);
      |
      |CREATE TABLE Users (
      | id VARCHAR(32) NOT NULL PRIMARY KEY
      |);
      |
      |CREATE TABLE Organizations (
      | id INTEGER NOT NULL PRIMARY KEY,
      | name TEXT NOT NULL
      |);
      |
      |CREATE TABLE Stores (
      | id INTEGER NOT NULL PRIMARY KEY,
      | name TEXT NOT NULL
      |);
      |
      |CREATE TABLE StoreRoles (
      | id INTEGER NOT NULL PRIMARY KEY
      |);
      |
      |findStoresForUser:
      |SELECT DISTINCT store.id, store.name
      |FROM StorePermissions AS permission
      |JOIN Stores AS store ON (permission.storeId = store.id)
      |WHERE permission.userId = ? AND permission.storeId IS NULL
      |ORDER BY store.name ASC
      |LIMIT ?
      |OFFSET ?;
    """.trimMargin(),
      tempFolder, dialect = TestDialect.MYSQL.dialect
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun findStoresForUser(
      |  userId: kotlin.String,
      |  value_: kotlin.Long,
      |  value__: kotlin.Long,
      |): app.cash.sqldelight.Query<com.example.Stores> = findStoresForUser(userId, value_, value__) { id, name ->
      |  com.example.Stores(
      |    id,
      |    name
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `query arguments are passed in correct order`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE sample (
      |  id INTEGER PRIMARY KEY,
      |  string TEXT,
      |  integer INTEGER
      |);
      |
      |query:
      |SELECT * FROM sample
      |WHERE
      |    (:someInteger IS NULL) OR (
      |        (string = :someString) AND (integer >= :someInteger)
      |    );
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
        |public fun <T : kotlin.Any> query(
        |  someInteger: kotlin.Long?,
        |  someString: kotlin.String?,
        |  mapper: (
        |    id: kotlin.Long,
        |    string: kotlin.String?,
        |    integer: kotlin.Long?,
        |  ) -> T,
        |): app.cash.sqldelight.Query<T> = QueryQuery(someInteger, someString) { cursor ->
        |  mapper(
        |    cursor.getLong(0)!!,
        |    cursor.getString(1),
        |    cursor.getLong(2)
        |  )
        |}
        |""".trimMargin()
    )
  }
}
