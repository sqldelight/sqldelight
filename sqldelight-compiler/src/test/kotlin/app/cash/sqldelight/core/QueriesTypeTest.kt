package app.cash.sqldelight.core

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
      temporaryFolder, fileName = "Data.sq"
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
      |import app.cash.sqldelight.db.SqlDriver
      |import com.example.DataQueries
      |import com.example.Data_
      |import com.example.Other
      |import com.example.TestDatabase
      |import kotlin.Int
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
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
      |  public override val dataQueries: DataQueries = DataQueries(driver, data_Adapter, otherAdapter)
      |
      |  public object Schema : SqlDriver.Schema {
      |    public override val version: Int
      |      get() = 1
      |
      |    public override fun create(driver: SqlDriver): Unit {
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
      |    }
      |
      |    public override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Int,
      |      newVersion: Int,
      |    ): Unit {
      |    }
      |  }
      |}
      |""".trimMargin()
    )

    val dataQueries = File(result.outputDirectory, "com/example/DataQueries.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.Unit
      |import kotlin.check
      |import kotlin.collections.List
      |import kotlin.collections.setOf
      |
      |public class DataQueries(
      |  private val driver: SqlDriver,
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
      |    return Query(424911250, arrayOf("data", "other"), driver, "Data.sq", "selectAllValues", ""${'"'}
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
      |  public fun insertData(id: Long?, value_: List?): Unit {
      |    driver.execute(${insert.id}, ""${'"'}
      |    |INSERT INTO data
      |    |VALUES (?, ?)
      |    ""${'"'}.trimMargin(), 2) {
      |      bindLong(1, id)
      |      bindString(2, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |    }
      |    notifyQueries(${insert.id}) { emit ->
      |      emit("data")
      |    }
      |  }
      |
      |  private inner class SelectForIdQuery<out T : Any>(
      |    public val id: Long,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    public override fun addListener(listener: Query.Listener): Unit {
      |      driver.addListener(listener, arrayOf("data"))
      |    }
      |
      |    public override fun removeListener(listener: Query.Listener): Unit {
      |      driver.removeListener(listener, arrayOf("data"))
      |    }
      |
      |    public override fun execute(): SqlCursor = driver.executeQuery(${select.id}, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id = ?
      |    ""${'"'}.trimMargin(), 1) {
      |      bindLong(1, id)
      |    }
      |
      |    public override fun toString(): String = "Data.sq:selectForId"
      |  }
      |}
      |""".trimMargin()
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
      temporaryFolder, fileName = "Data.sq"
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
      |import app.cash.sqldelight.db.SqlDriver
      |import com.example.DataQueries
      |import com.example.Data_
      |import com.example.TestDatabase
      |import kotlin.Int
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver, data_Adapter: Data_.Adapter):
      |    TestDatabase = TestDatabaseImpl(driver, data_Adapter)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |  data_Adapter: Data_.Adapter,
      |) : TransacterImpl(driver), TestDatabase {
      |  public override val dataQueries: DataQueries = DataQueries(driver, data_Adapter)
      |
      |  public object Schema : SqlDriver.Schema {
      |    public override val version: Int
      |      get() = 1
      |
      |    public override fun create(driver: SqlDriver): Unit {
      |      driver.execute(null, ""${'"'}
      |          |CREATE VIRTUAL TABLE data USING fts5(
      |          |  id,
      |          |  value
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |    }
      |
      |    public override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Int,
      |      newVersion: Int,
      |    ): Unit {
      |    }
      |  }
      |}
      |""".trimMargin()
    )

    val dataQueries = File(result.outputDirectory, "com/example/DataQueries.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.Unit
      |import kotlin.collections.List
      |
      |public class DataQueries(
      |  private val driver: SqlDriver,
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
      |  public fun selectForId(id: Long): Query<Data_> = selectForId(id) { id_, value_ ->
      |    Data_(
      |      id_,
      |      value_
      |    )
      |  }
      |
      |  public fun insertData(id: Long?, value_: List?): Unit {
      |    driver.execute(${insert.id}, ""${'"'}
      |    |INSERT INTO data
      |    |VALUES (?, ?)
      |    ""${'"'}.trimMargin(), 2) {
      |      bindLong(1, id)
      |      bindString(2, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |    }
      |    notifyQueries(${insert.id}) { emit ->
      |      emit("data")
      |    }
      |  }
      |
      |  private inner class SelectForIdQuery<out T : Any>(
      |    public val id: Long,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    public override fun addListener(listener: Query.Listener): Unit {
      |      driver.addListener(listener, arrayOf("data"))
      |    }
      |
      |    public override fun removeListener(listener: Query.Listener): Unit {
      |      driver.removeListener(listener, arrayOf("data"))
      |    }
      |
      |    public override fun execute(): SqlCursor = driver.executeQuery(${select.id}, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id = ?
      |    ""${'"'}.trimMargin(), 1) {
      |      bindLong(1, id)
      |    }
      |
      |    public override fun toString(): String = "Data.sq:selectForId"
      |  }
      |}
      |""".trimMargin()
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
      temporaryFolder, fileName = "Search.sq"
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
      |import app.cash.sqldelight.db.SqlDriver
      |import com.example.SearchQueries
      |import com.example.TestDatabase
      |import kotlin.Int
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
      |    TestDatabaseImpl(driver)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver), TestDatabase {
      |  public override val searchQueries: SearchQueries = SearchQueries(driver)
      |
      |  public object Schema : SqlDriver.Schema {
      |    public override val version: Int
      |      get() = 1
      |
      |    public override fun create(driver: SqlDriver): Unit {
      |      driver.execute(null, ""${'"'}
      |          |CREATE VIRTUAL TABLE search USING fts3(
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |    }
      |
      |    public override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Int,
      |      newVersion: Int,
      |    ): Unit {
      |    }
      |  }
      |}
      |""".trimMargin()
    )

    val dataQueries = File(result.outputDirectory, "com/example/SearchQueries.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.Unit
      |
      |public class SearchQueries(
      |  private val driver: SqlDriver,
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
      |  public fun insertData(id: Long?, value_: String?): Unit {
      |    driver.execute(${insert.id}, ""${'"'}
      |    |INSERT INTO search
      |    |VALUES (?, ?)
      |    ""${'"'}.trimMargin(), 2) {
      |      bindLong(1, id)
      |      bindString(2, value_)
      |    }
      |    notifyQueries(${insert.id}) { emit ->
      |      emit("search")
      |    }
      |  }
      |
      |  private inner class SelectOffsetsQuery<out T : Any>(
      |    public val search: String,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    public override fun addListener(listener: Query.Listener): Unit {
      |      driver.addListener(listener, arrayOf("search"))
      |    }
      |
      |    public override fun removeListener(listener: Query.Listener): Unit {
      |      driver.removeListener(listener, arrayOf("search"))
      |    }
      |
      |    public override fun execute(): SqlCursor = driver.executeQuery(${offsets.id}, ""${'"'}
      |    |SELECT id, offsets(search)
      |    |FROM search
      |    |WHERE search MATCH ?
      |    ""${'"'}.trimMargin(), 1) {
      |      bindString(1, search)
      |    }
      |
      |    public override fun toString(): String = "Search.sq:selectOffsets"
      |  }
      |}
      |""".trimMargin()
    )
  }
}
