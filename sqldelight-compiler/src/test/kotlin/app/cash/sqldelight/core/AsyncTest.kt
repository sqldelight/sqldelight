package app.cash.sqldelight.core

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AsyncTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `queries file is generated properly via compilation`() {
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
      temporaryFolder, fileName = "Data.sq",
      generateAsync = true
    )

    val select = result.compiledFile.namedQueries.first()
    val insert = result.compiledFile.namedMutators.first()
    Truth.assertThat(result.errors).isEmpty()

    val database = File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")
    Truth.assertThat(result.compilerOutput).containsKey(database)
    Truth.assertThat(result.compilerOutput[database].toString()).isEqualTo(
      """
      |package com.example.testmodule
      |
      |import app.cash.sqldelight.async.AsyncTransacterImpl
      |import app.cash.sqldelight.async.db.AsyncSqlDriver
      |import app.cash.sqldelight.async.db.combine
      |import com.example.DataQueries
      |import com.example.Data_
      |import com.example.Other
      |import com.example.TestDatabase
      |import kotlin.Int
      |import kotlin.Long
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: AsyncSqlDriver.Schema
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(
      |  driver: AsyncSqlDriver,
      |  data_Adapter: Data_.Adapter,
      |  otherAdapter: Other.Adapter,
      |): TestDatabase = TestDatabaseImpl(driver, data_Adapter, otherAdapter)
      |
      |private class TestDatabaseImpl(
      |  driver: AsyncSqlDriver,
      |  data_Adapter: Data_.Adapter,
      |  otherAdapter: Other.Adapter,
      |) : AsyncTransacterImpl(driver), TestDatabase {
      |  public override val dataQueries: DataQueries = DataQueries(driver, data_Adapter, otherAdapter)
      |
      |  public object Schema : AsyncSqlDriver.Schema {
      |    public override val version: Int
      |      get() = 1
      |
      |    public override fun create(driver: AsyncSqlDriver): AsyncSqlDriver.Callback<Unit> {
      |      val statements = mutableListOf<AsyncSqlDriver.Callback<Long>>()
      |      statements.add(driver.execute(null, ""${'"'}
      |          |CREATE TABLE data (
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0))
      |      statements.add(driver.execute(null, ""${'"'}
      |          |CREATE TABLE other (
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0))
      |      return statements.combine()
      |    }
      |
      |    public override fun migrate(
      |      driver: AsyncSqlDriver,
      |      oldVersion: Int,
      |      newVersion: Int,
      |    ): AsyncSqlDriver.Callback<Unit> {
      |      val statements = mutableListOf<AsyncSqlDriver.Callback<Long>>()
      |      return statements.combine()
      |    }
      |  }
      |}
      |""".trimMargin()
    )

    val dataQueries = File(result.outputDirectory, "com/example/DataQueries.kt")
    Truth.assertThat(result.compilerOutput).containsKey(dataQueries)
    Truth.assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.async.AsyncQuery
      |import app.cash.sqldelight.async.AsyncTransacterImpl
      |import app.cash.sqldelight.async.Query
      |import app.cash.sqldelight.async.db.AsyncSqlDriver
      |import app.cash.sqldelight.async.db.SqlCursor
      |import app.cash.sqldelight.async.db.map
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.Throwable
      |import kotlin.Unit
      |import kotlin.check
      |import kotlin.collections.List
      |import kotlin.collections.setOf
      |
      |public class DataQueries(
      |  private val driver: AsyncSqlDriver,
      |  private val data_Adapter: Data_.Adapter,
      |  private val otherAdapter: Other.Adapter,
      |) : AsyncTransacterImpl(driver) {
      |  public fun <T : Any> selectForId(id: Long, mapper: (id: Long, value_: List?) -> T): AsyncQuery<T>
      |      = SelectForIdQuery(id) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1)?.let { data_Adapter.value_Adapter.decode(it) }
      |    )
      |  }
      |
      |  public fun selectForId(id: Long): AsyncQuery<Data_> = selectForId(id) { id_, value_ ->
      |    Data_(
      |      id_,
      |      value_
      |    )
      |  }
      |
      |  public fun <T : Any> selectAllValues(mapper: (id: Long, value_: List?) -> T): AsyncQuery<T> {
      |    check(setOf(dataAdapter.value_Adapter, otherAdapter.value_Adapter).size == 1) {
      |        "Adapter types are expected to be identical." }
      |    return AsyncQuery(424911250, arrayOf("data", "other"), driver, "Data.sq", "selectAllValues", ""${'"'}
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
      |  public fun selectAllValues(): AsyncQuery<SelectAllValues> = selectAllValues { id, value_ ->
      |    SelectAllValues(
      |      id,
      |      value_
      |    )
      |  }
      |
      |  public fun insertData(id: Long?, value_: List?): AsyncSqlDriver.Callback<Unit> =
      |      driver.execute(${insert.id}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        bindLong(1, id)
      |        bindString(2, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }
      |  .onSuccess {
      |    notifyQueries(${insert.id}) { emit ->
      |      emit("data")
      |    }
      |  }
      |  .map {}
      |
      |  private inner class SelectForIdQuery<out T : Any>(
      |    public val id: Long,
      |    mapper: (SqlCursor) -> T,
      |  ) : AsyncQuery<T>(mapper) {
      |    public override fun addListener(listener: Query.Listener): Unit {
      |      driver.addListener(listener, arrayOf("data"))
      |    }
      |
      |    public override fun removeListener(listener: Query.Listener): Unit {
      |      driver.removeListener(listener, arrayOf("data"))
      |    }
      |
      |    public override fun <R> execute(
      |      onSuccess: (R) -> Unit,
      |      onError: (Throwable) -> Unit,
      |      mapper: (SqlCursor) -> R,
      |    ): AsyncSqlDriver.Callback<R> = driver.executeQuery(${select.id}, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id = ?
      |    ""${'"'}.trimMargin(), mapper, 1) {
      |      bindLong(1, id)
      |    }
      |
      |    public override fun toString(): String = "Data.sq:selectForId"
      |  }
      |}
      |""".trimMargin()
    )
  }
}
