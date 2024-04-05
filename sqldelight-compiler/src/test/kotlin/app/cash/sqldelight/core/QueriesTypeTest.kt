package app.cash.sqldelight.core

import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.fixtureRoot
import app.cash.sqldelight.test.util.withUnderscores
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QueriesTypeTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun `queries file is generated properly via compilation`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |CREATE TABLE other (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      |
      |selectAllValues:
      |SELECT id, value FROM data
      |UNION
      |SELECT id, value FROM other;
      """.trimMargin(),
      temporaryFolder,
      fileName = "Data.sq",
    )

    val select = result.compiledFile.namedQueries.first()
    val insert = result.compiledFile.namedMutators.first()
    assertThat(result.errors).isEmpty()

    val database = File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")
    assertThat(result.compilerOutput).containsKey(database)
    assertThat(result.compilerOutput[database].toString()).isEqualTo(
      """
      |package com.example.testmodule
      |
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.AfterVersion
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlDriver
      |import app.cash.sqldelight.db.SqlSchema
      |import com.example.DataQueries
      |import com.example.Data_
      |import com.example.Other
      |import com.example.TestDatabase
      |import kotlin.Long
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(
      |  driver: SqlDriver,
      |  data_Adapter: Data_.Adapter,
      |  otherAdapter: Other.Adapter,
      |): TestDatabase = TestDatabaseImpl(driver, data_Adapter, otherAdapter)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |  data_Adapter: Data_.Adapter,
      |  otherAdapter: Other.Adapter,
      |) : TransacterImpl(driver), TestDatabase {
      |  override val dataQueries: DataQueries = DataQueries(driver, data_Adapter, otherAdapter)
      |
      |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
      |    override val version: Long
      |      get() = 1
      |
      |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      |      driver.execute(null, ""${'"'}
      |          |CREATE TABLE data (
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |      driver.execute(null, ""${'"'}
      |          |CREATE TABLE other (
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |      return QueryResult.Unit
      |    }
      |
      |    override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Long,
      |      newVersion: Long,
      |      vararg callbacks: AfterVersion,
      |    ): QueryResult.Value<Unit> = QueryResult.Unit
      |  }
      |}
      |
      """.trimMargin(),
    )

    val dataQueries = File(result.outputDirectory, "com/example/DataQueries.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.check
      |import kotlin.collections.List
      |import kotlin.collections.setOf
      |
      |public class DataQueries(
      |  driver: SqlDriver,
      |  private val data_Adapter: Data_.Adapter,
      |  private val otherAdapter: Other.Adapter,
      |) : TransacterImpl(driver) {
      |  public fun <T : Any> selectForId(id: Long, mapper: (id: Long, value_: List?) -> T): Query<T> =
      |      SelectForIdQuery(id) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1)?.let { data_Adapter.value_Adapter.decode(it) }
      |    )
      |  }
      |
      |  public fun selectForId(id: Long): Query<Data_> = selectForId(id) { id_, value_ ->
      |    Data_(
      |      id_,
      |      value_
      |    )
      |  }
      |
      |  public fun <T : Any> selectAllValues(mapper: (id: Long, value_: List?) -> T): Query<T> {
      |    check(setOf(dataAdapter.value_Adapter, otherAdapter.value_Adapter).size == 1) {
      |        "Adapter types are expected to be identical." }
      |    return Query(424_911_250, arrayOf("data", "other"), driver, "Data.sq", "selectAllValues", ""${'"'}
      |    |SELECT id, value FROM data
      |    |UNION
      |    |SELECT id, value FROM other
      |    ""${'"'}.trimMargin()) { cursor ->
      |      mapper(
      |        cursor.getLong(0)!!,
      |        cursor.getString(1)?.let { data_Adapter.value_Adapter.decode(it) }
      |      )
      |    }
      |  }
      |
      |  public fun selectAllValues(): Query<SelectAllValues> = selectAllValues { id, value_ ->
      |    SelectAllValues(
      |      id,
      |      value_
      |    )
      |  }
      |
      |  public fun insertData(id: Long?, value_: List?) {
      |    driver.execute(${insert.id.withUnderscores}, ""${'"'}
      |        |INSERT INTO data
      |        |VALUES (?, ?)
      |        ""${'"'}.trimMargin(), 2) {
      |          bindLong(0, id)
      |          bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |        }
      |    notifyQueries(${insert.id.withUnderscores}) { emit ->
      |      emit("data")
      |    }
      |  }
      |
      |  private inner class SelectForIdQuery<out T : Any>(
      |    public val id: Long,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("data", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("data", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
      |        driver.executeQuery(${select.id.withUnderscores}, ""${'"'}
      |    |SELECT data.id, data.value
      |    |FROM data
      |    |WHERE id = ?
      |    ""${'"'}.trimMargin(), mapper, 1) {
      |      bindLong(0, id)
      |    }
      |
      |    override fun toString(): String = "Data.sq:selectForId"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `queries file is generated properly with adapter`() {
    val result = FixtureCompiler.compileSql(
      """
      |import foo.S;
      |
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS S.Bar
      |);
      |
      |
      |insertData:
      |INSERT INTO data
      |VALUES ?;
      """.trimMargin(),
      temporaryFolder,
      fileName = "Data.sq",
    )
    val insert = result.compiledFile.namedMutators.first()
    assertThat(result.errors).isEmpty()

    val database = File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")
    assertThat(result.compilerOutput).containsKey(database)
    assertThat(result.compilerOutput[database].toString()).isEqualTo(
      """
      |package com.example.testmodule
      |
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.AfterVersion
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlDriver
      |import app.cash.sqldelight.db.SqlSchema
      |import com.example.DataQueries
      |import com.example.Data_
      |import com.example.TestDatabase
      |import kotlin.Long
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver, data_Adapter: Data_.Adapter):
      |    TestDatabase = TestDatabaseImpl(driver, data_Adapter)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |  data_Adapter: Data_.Adapter,
      |) : TransacterImpl(driver), TestDatabase {
      |  override val dataQueries: DataQueries = DataQueries(driver, data_Adapter)
      |
      |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
      |    override val version: Long
      |      get() = 1
      |
      |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      |      driver.execute(null, ""${'"'}
      |          |CREATE TABLE data (
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |      return QueryResult.Unit
      |    }
      |
      |    override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Long,
      |      newVersion: Long,
      |      vararg callbacks: AfterVersion,
      |    ): QueryResult.Value<Unit> = QueryResult.Unit
      |  }
      |}
      |
      """.trimMargin(),
    )

    val dataQueries = File(result.outputDirectory, "com/example/DataQueries.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.SqlDriver
      |
      |public class DataQueries(
      |  driver: SqlDriver,
      |  private val data_Adapter: Data_.Adapter,
      |) : TransacterImpl(driver) {
      |  public fun insertData(data_: Data_) {
      |    driver.execute(${insert.id.withUnderscores}, ""${'"'}
      |        |INSERT INTO data (id, value)
      |        |VALUES (?, ?)
      |        ""${'"'}.trimMargin(), 2) {
      |          bindLong(0, data_.id)
      |          bindString(1, data_.value_?.let { data_Adapter.value_Adapter.encode(it) })
      |        }
      |    notifyQueries(${insert.id.withUnderscores}) { emit ->
      |      emit("data")
      |    }
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `unused adapters are not passed to the database constructor`() {
    val result = FixtureCompiler.compileSql(
      """
      |import kotlin.Int;
      |
      |CREATE TABLE data (
      |  id TEXT PRIMARY KEY,
      |  value INTEGER AS Int NOT NULL
      |);
      """.trimMargin(),
      temporaryFolder,
      fileName = "Data.sq",
    )

    val database = File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")
    assertThat(result.compilerOutput).containsKey(database)
    assertThat(result.compilerOutput[database].toString()).isEqualTo(
      """
      |package com.example.testmodule
      |
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.AfterVersion
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlDriver
      |import app.cash.sqldelight.db.SqlSchema
      |import com.example.TestDatabase
      |import kotlin.Long
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
      |    TestDatabaseImpl(driver)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver), TestDatabase {
      |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
      |    override val version: Long
      |      get() = 1
      |
      |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      |      driver.execute(null, ""${'"'}
      |          |CREATE TABLE data (
      |          |  id TEXT PRIMARY KEY,
      |          |  value INTEGER NOT NULL
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |      return QueryResult.Unit
      |    }
      |
      |    override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Long,
      |      newVersion: Long,
      |      vararg callbacks: AfterVersion,
      |    ): QueryResult.Value<Unit> = QueryResult.Unit
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `queries file is generated properly via compilation1a`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE VIRTUAL TABLE data USING fts5(
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      temporaryFolder,
      fileName = "Data.sq",
    )

    val select = result.compiledFile.namedQueries.first()
    val insert = result.compiledFile.namedMutators.first()
    assertThat(result.errors).isEmpty()

    val database = File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")
    assertThat(result.compilerOutput).containsKey(database)
    assertThat(result.compilerOutput[database].toString()).isEqualTo(
      """
      |package com.example.testmodule
      |
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.AfterVersion
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlDriver
      |import app.cash.sqldelight.db.SqlSchema
      |import com.example.DataQueries
      |import com.example.Data_
      |import com.example.TestDatabase
      |import kotlin.Long
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver, data_Adapter: Data_.Adapter):
      |    TestDatabase = TestDatabaseImpl(driver, data_Adapter)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |  data_Adapter: Data_.Adapter,
      |) : TransacterImpl(driver), TestDatabase {
      |  override val dataQueries: DataQueries = DataQueries(driver, data_Adapter)
      |
      |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
      |    override val version: Long
      |      get() = 1
      |
      |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      |      driver.execute(null, ""${'"'}
      |          |CREATE VIRTUAL TABLE data USING fts5(
      |          |  id,
      |          |  value
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |      return QueryResult.Unit
      |    }
      |
      |    override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Long,
      |      newVersion: Long,
      |      vararg callbacks: AfterVersion,
      |    ): QueryResult.Value<Unit> = QueryResult.Unit
      |  }
      |}
      |
      """.trimMargin(),
    )

    val dataQueries = File(result.outputDirectory, "com/example/DataQueries.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.collections.List
      |
      |public class DataQueries(
      |  driver: SqlDriver,
      |  private val data_Adapter: Data_.Adapter,
      |) : TransacterImpl(driver) {
      |  public fun <T : Any> selectForId(id: Long, mapper: (id: Long, value_: List?) -> T): Query<T> =
      |      SelectForIdQuery(id) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1)?.let { data_Adapter.value_Adapter.decode(it) }
      |    )
      |  }
      |
      |  public fun selectForId(id: Long): Query<SelectForId> = selectForId(id) { id_, value_ ->
      |    SelectForId(
      |      id_,
      |      value_
      |    )
      |  }
      |
      |  public fun insertData(id: Long?, value_: List?) {
      |    driver.execute(${insert.id.withUnderscores}, ""${'"'}
      |        |INSERT INTO data
      |        |VALUES (?, ?)
      |        ""${'"'}.trimMargin(), 2) {
      |          bindLong(0, id)
      |          bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |        }
      |    notifyQueries(${insert.id.withUnderscores}) { emit ->
      |      emit("data")
      |    }
      |  }
      |
      |  private inner class SelectForIdQuery<out T : Any>(
      |    public val id: Long,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("data", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("data", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
      |        driver.executeQuery(${select.id.withUnderscores}, ""${'"'}
      |    |SELECT data.id, data.value
      |    |FROM data
      |    |WHERE id = ?
      |    ""${'"'}.trimMargin(), mapper, 1) {
      |      bindLong(0, id)
      |    }
      |
      |    override fun toString(): String = "Data.sq:selectForId"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `queries file is generated properly via compilation with offsets`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE VIRTUAL TABLE search USING fts3(
      |  id INTEGER PRIMARY KEY,
      |  value TEXT
      |);
      |
      |insertData:
      |INSERT INTO search
      |VALUES (?, ?);
      |
      |selectOffsets:
      |SELECT id, offsets(search)
      |FROM search
      |WHERE search MATCH ?;
      """.trimMargin(),
      temporaryFolder,
      fileName = "Search.sq",
    )

    val offsets = result.compiledFile.namedQueries.first()
    val insert = result.compiledFile.namedMutators.first()
    assertThat(result.errors).isEmpty()

    val database = File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")
    assertThat(result.compilerOutput).containsKey(database)
    assertThat(result.compilerOutput[database].toString()).isEqualTo(
      """
      |package com.example.testmodule
      |
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.AfterVersion
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlDriver
      |import app.cash.sqldelight.db.SqlSchema
      |import com.example.SearchQueries
      |import com.example.TestDatabase
      |import kotlin.Long
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
      |    TestDatabaseImpl(driver)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver), TestDatabase {
      |  override val searchQueries: SearchQueries = SearchQueries(driver)
      |
      |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
      |    override val version: Long
      |      get() = 1
      |
      |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      |      driver.execute(null, ""${'"'}
      |          |CREATE VIRTUAL TABLE search USING fts3(
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |      return QueryResult.Unit
      |    }
      |
      |    override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Long,
      |      newVersion: Long,
      |      vararg callbacks: AfterVersion,
      |    ): QueryResult.Value<Unit> = QueryResult.Unit
      |  }
      |}
      |
      """.trimMargin(),
    )

    val dataQueries = File(result.outputDirectory, "com/example/SearchQueries.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |
      |public class SearchQueries(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver) {
      |  public fun <T : Any> selectOffsets(search: String, mapper: (id: Long, offsets: String?) -> T):
      |      Query<T> = SelectOffsetsQuery(search) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1)
      |    )
      |  }
      |
      |  public fun selectOffsets(search: String): Query<SelectOffsets> = selectOffsets(search) { id,
      |      offsets ->
      |    SelectOffsets(
      |      id,
      |      offsets
      |    )
      |  }
      |
      |  public fun insertData(id: Long?, value_: String?) {
      |    driver.execute(${insert.id.withUnderscores}, ""${'"'}
      |        |INSERT INTO search
      |        |VALUES (?, ?)
      |        ""${'"'}.trimMargin(), 2) {
      |          bindLong(0, id)
      |          bindString(1, value_)
      |        }
      |    notifyQueries(${insert.id.withUnderscores}) { emit ->
      |      emit("search")
      |    }
      |  }
      |
      |  private inner class SelectOffsetsQuery<out T : Any>(
      |    public val search: String,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("search", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("search", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
      |        driver.executeQuery(${offsets.id.withUnderscores}, ""${'"'}
      |    |SELECT id, offsets(search)
      |    |FROM search
      |    |WHERE search MATCH ?
      |    ""${'"'}.trimMargin(), mapper, 1) {
      |      bindString(0, search)
      |    }
      |
      |    override fun toString(): String = "Search.sq:selectOffsets"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `adapter through view resolves correctly`() {
    FixtureCompiler.writeSql(
      """
      |import com.chicken.SoupBase.Broth;
      |
      |CREATE TABLE soupBase(
      |  token TEXT NOT NULL PRIMARY KEY,
      |  soup_token TEXT NOT NULL,
      |  soup_broth BLOB AS Broth
      |);
      """.trimMargin(),
      temporaryFolder,
      fileName = "db/SoupBase.sq",
    )

    FixtureCompiler.writeSql(
      """
      |import com.chicken.Soup.SoupName;
      |
      |CREATE TABLE soup(
      |  token TEXT NOT NULL PRIMARY KEY,
      |  soup_name BLOB AS SoupName
      |);
      """.trimMargin(),
      temporaryFolder,
      fileName = "db/Soup.sq",
    )

    FixtureCompiler.writeSql(
      """
      |CREATE VIEW soupView AS
      |SELECT soupBase.*,
      |       soup.soup_name
      |FROM soupBase
      |LEFT JOIN soup ON soup.token = soupBase.soup_token;
      |
      |forSoupToken:
      |SELECT *
      |FROM soupView
      |WHERE soup_token = ?;
      |
      |maxSoupBroth:
      |SELECT MAX(soup_broth)
      |FROM soupView;
      """.trimMargin(),
      temporaryFolder,
      fileName = "MyView.sq",
    )

    val result = FixtureCompiler.compileFixture(temporaryFolder.fixtureRoot().path)
    assertThat(result.errors).isEmpty()

    val dataQueries = File(result.outputDirectory, "com/example/MyViewQueries.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.String
      |import com.chicken.SoupBase as ChickenSoupBase
      |import com.chicken.Soup as ChickenSoup
      |import com.example.db.SoupBase as DbSoupBase
      |import com.example.db.Soup as DbSoup
      |
      |public class MyViewQueries(
      |  driver: SqlDriver,
      |  private val soupBaseAdapter: DbSoupBase.Adapter,
      |  private val soupAdapter: DbSoup.Adapter,
      |) : TransacterImpl(driver) {
      |  public fun <T : Any> forSoupToken(soup_token: String, mapper: (
      |    token: String,
      |    soup_token: String,
      |    soup_broth: ChickenSoupBase.Broth?,
      |    soup_name: ChickenSoup.SoupName?,
      |  ) -> T): Query<T> = ForSoupTokenQuery(soup_token) { cursor ->
      |    mapper(
      |      cursor.getString(0)!!,
      |      cursor.getString(1)!!,
      |      cursor.getBytes(2)?.let { soupBaseAdapter.soup_brothAdapter.decode(it) },
      |      cursor.getBytes(3)?.let { soupAdapter.soup_nameAdapter.decode(it) }
      |    )
      |  }
      |
      |  public fun forSoupToken(soup_token: String): Query<SoupView> = forSoupToken(soup_token) { token,
      |      soup_token_, soup_broth, soup_name ->
      |    SoupView(
      |      token,
      |      soup_token_,
      |      soup_broth,
      |      soup_name
      |    )
      |  }
      |
      |  public fun <T : Any> maxSoupBroth(mapper: (MAX: ChickenSoupBase.Broth?) -> T): Query<T> =
      |      Query(-1_892_940_684, arrayOf("soupBase", "soup"), driver, "MyView.sq", "maxSoupBroth", ""${'"'}
      |  |SELECT MAX(soup_broth)
      |  |FROM soupView
      |  ""${'"'}.trimMargin()) { cursor ->
      |    mapper(
      |      cursor.getBytes(0)?.let { soupBaseAdapter.soup_brothAdapter.decode(it) }
      |    )
      |  }
      |
      |  public fun maxSoupBroth(): Query<MaxSoupBroth> = maxSoupBroth { MAX ->
      |    MaxSoupBroth(
      |      MAX
      |    )
      |  }
      |
      |  private inner class ForSoupTokenQuery<out T : Any>(
      |    public val soup_token: String,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("soupBase", "soup", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("soupBase", "soup", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
      |        driver.executeQuery(-988_424_235, ""${'"'}
      |    |SELECT soupView.token, soupView.soup_token, soupView.soup_broth, soupView.soup_name
      |    |FROM soupView
      |    |WHERE soup_token = ?
      |    ""${'"'}.trimMargin(), mapper, 1) {
      |      bindString(0, soup_token)
      |    }
      |
      |    override fun toString(): String = "MyView.sq:forSoupToken"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `grouped statement with return and no arguments gets a query type`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT
      |);
      |
      |insertAndReturn {
      |  INSERT INTO data (value)
      |  VALUES (NULL)
      |  ;
      |  SELECT last_insert_rowid();
      |}
      """.trimMargin(),
      temporaryFolder,
      fileName = "Data.sq",
    )

    val query = result.compiledFile.namedQueries.first()
    assertThat(result.errors).isEmpty()

    val dataQueries = File(result.outputDirectory, "com/example/DataQueries.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.ExecutableQuery
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |
      |public class DataQueries(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver) {
      |  public fun insertAndReturn(): ExecutableQuery<Long> = InsertAndReturnQuery() { cursor ->
      |    cursor.getLong(0)!!
      |  }
      |
      |  private inner class InsertAndReturnQuery<out T : Any>(
      |    mapper: (SqlCursor) -> T,
      |  ) : ExecutableQuery<T>(mapper) {
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
      |        transactionWithResult {
      |      driver.execute(${query.idForIndex(0).withUnderscores}, ""${'"'}
      |          |INSERT INTO data (value)
      |          |  VALUES (NULL)
      |          ""${'"'}.trimMargin(), 0)
      |      driver.executeQuery(${query.idForIndex(1).withUnderscores}, ""${'"'}SELECT last_insert_rowid()""${'"'}, mapper, 0)
      |    }
      |
      |    override fun toString(): String = "Data.sq:insertAndReturn"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }
}
