package com.squareup.sqldelight.core

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.test.util.FixtureCompiler
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

enum class EnumClass { TYPE_A }

class QueriesTypeTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun `queries file is generated properly via compilation`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE data (
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
    """.trimMargin(), temporaryFolder, fileName = "Data.sq")

    val select = result.compiledFile.namedQueries.first()
    val insert = result.compiledFile.namedMutators.first()
    assertThat(result.errors).isEmpty()

    val dataQueries = File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo("""
      |package com.example.testmodule
      |
      |import com.example.Data
      |import com.example.DataQueries
      |import com.example.TestDatabase
      |import com.squareup.sqldelight.Query
      |import com.squareup.sqldelight.TransacterImpl
      |import com.squareup.sqldelight.db.SqlCursor
      |import com.squareup.sqldelight.db.SqlDriver
      |import com.squareup.sqldelight.internal.copyOnWriteList
      |import kotlin.Any
      |import kotlin.Int
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.collections.List
      |import kotlin.collections.MutableList
      |import kotlin.jvm.JvmField
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver, dataAdapter: Data.Adapter):
      |    TestDatabase = TestDatabaseImpl(driver, dataAdapter)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |  internal val dataAdapter: Data.Adapter
      |) : TransacterImpl(driver), TestDatabase {
      |  override val dataQueries: DataQueriesImpl = DataQueriesImpl(this, driver)
      |
      |  object Schema : SqlDriver.Schema {
      |    override val version: Int
      |      get() = 1
      |
      |    override fun create(driver: SqlDriver) {
      |      driver.execute(null, ""${'"'}
      |          |CREATE TABLE data (
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |    }
      |
      |    override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Int,
      |      newVersion: Int
      |    ) {
      |    }
      |  }
      |}
      |
      |private class DataQueriesImpl(
      |  private val database: TestDatabaseImpl,
      |  private val driver: SqlDriver
      |) : TransacterImpl(driver), DataQueries {
      |  internal val selectForId: MutableList<Query<*>> = copyOnWriteList()
      |
      |  override fun <T : Any> selectForId(id: Long, mapper: (id: Long, value: List?) -> T): Query<T> =
      |      SelectForIdQuery(id) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1)?.let(database.dataAdapter.valueAdapter::decode)
      |    )
      |  }
      |
      |  override fun selectForId(id: Long): Query<Data> = selectForId(id, ::Data)
      |
      |  override fun insertData(id: Long?, value: List?) {
      |    driver.execute(${insert.id}, ""${'"'}
      |    |INSERT INTO data
      |    |VALUES (?, ?)
      |    ""${'"'}.trimMargin(), 2) {
      |      bindLong(1, id)
      |      bindString(2, if (value == null) null else database.dataAdapter.valueAdapter.encode(value))
      |    }
      |    notifyQueries(${insert.id}, {database.dataQueries.selectForId})
      |  }
      |
      |  private inner class SelectForIdQuery<out T : Any>(
      |    @JvmField
      |    val id: Long,
      |    mapper: (SqlCursor) -> T
      |  ) : Query<T>(selectForId, mapper) {
      |    override fun execute(): SqlCursor = driver.executeQuery(${select.id}, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id = ?
      |    ""${'"'}.trimMargin(), 1) {
      |      bindLong(1, id)
      |    }
      |
      |    override fun toString(): String = "Data.sq:selectForId"
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun `queries file is generated properly via compilation2`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE VIRTUAL TABLE data2 USING fts5(
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List,
      |  enum TEXT as EnumClass
      |);
      |
      |insertData:
      |INSERT INTO data2
      |VALUES (?, ?, ?);
      |
      |selectForId:
      |SELECT *
      |FROM data2
      |WHERE id = ?;
    """.trimMargin(), temporaryFolder, fileName = "Data2.sq")

    val select = result.compiledFile.namedQueries.first()
    val insert = result.compiledFile.namedMutators.first()
    assertThat(result.errors).isEmpty()

    val dataQueries = File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")
    assertThat(result.compilerOutput).containsKey(dataQueries)
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo("""
      |package com.example.testmodule
      |
      |import EnumClass
      |import com.example.Data2
      |import com.example.Data2Queries
      |import com.example.TestDatabase
      |import com.squareup.sqldelight.Query
      |import com.squareup.sqldelight.TransacterImpl
      |import com.squareup.sqldelight.db.SqlCursor
      |import com.squareup.sqldelight.db.SqlDriver
      |import com.squareup.sqldelight.internal.copyOnWriteList
      |import kotlin.Any
      |import kotlin.Int
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.collections.List
      |import kotlin.collections.MutableList
      |import kotlin.jvm.JvmField
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver, data2Adapter: Data2.Adapter):
      |    TestDatabase = TestDatabaseImpl(driver, data2Adapter)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |  internal val data2Adapter: Data2.Adapter
      |) : TransacterImpl(driver), TestDatabase {
      |  override val data2Queries: Data2QueriesImpl = Data2QueriesImpl(this, driver)
      |
      |  object Schema : SqlDriver.Schema {
      |    override val version: Int
      |      get() = 1
      |
      |    override fun create(driver: SqlDriver) {
      |      driver.execute(null, ""${'"'}
      |          |CREATE VIRTUAL TABLE data2 USING fts5(
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT,
      |          |  enum TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |    }
      |
      |    override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Int,
      |      newVersion: Int
      |    ) {
      |    }
      |  }
      |}
      |
      |private class Data2QueriesImpl(
      |  private val database: TestDatabaseImpl,
      |  private val driver: SqlDriver
      |) : TransacterImpl(driver), Data2Queries {
      |  internal val selectForId: MutableList<Query<*>> = copyOnWriteList()
      |
      |  override fun <T : Any> selectForId(id: Long, mapper: (
      |    id: Long,
      |    value: List?,
      |    enum: EnumClass?
      |  ) -> T): Query<T> = SelectForIdQuery(id) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1)?.let(database.data2Adapter.valueAdapter::decode),
      |      cursor.getString(2)?.let(database.data2Adapter.enumAdapter::decode)
      |    )
      |  }
      |
      |  override fun selectForId(id: Long): Query<Data2> = selectForId(id, ::Data2)
      |
      |  override fun insertData(
      |    id: Long?,
      |    value: List?,
      |    enum: EnumClass?
      |  ) {
      |    driver.execute(${insert.id}, ""${'"'}
      |    |INSERT INTO data2
      |    |VALUES (?, ?, ?)
      |    ""${'"'}.trimMargin(), 3) {
      |      bindLong(1, id)
      |      bindString(2, if (value == null) null else database.data2Adapter.valueAdapter.encode(value))
      |      bindString(3, if (enum == null) null else database.data2Adapter.enumAdapter.encode(enum))
      |    }
      |    notifyQueries(${insert.id}, {database.data2Queries.selectForId})
      |  }
      |
      |  private inner class SelectForIdQuery<out T : Any>(
      |    @JvmField
      |    val id: Long,
      |    mapper: (SqlCursor) -> T
      |  ) : Query<T>(selectForId, mapper) {
      |    override fun execute(): SqlCursor = driver.executeQuery(${select.id}, ""${'"'}
      |    |SELECT *
      |    |FROM data2
      |    |WHERE id = ?
      |    ""${'"'}.trimMargin(), 1) {
      |      bindLong(1, id)
      |    }
      |
      |    override fun toString(): String = "Data2.sq:selectForId"
      |  }
      |}
      |""".trimMargin())
    }
}

