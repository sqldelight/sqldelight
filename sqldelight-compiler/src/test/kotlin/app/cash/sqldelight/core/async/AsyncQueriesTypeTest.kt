package app.cash.sqldelight.core.async

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AsyncQueriesTypeTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test
  fun `queries file is generated with suspending transacter`() {
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
      generateAsync = true,
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
      |import app.cash.sqldelight.SuspendingTransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlDriver
      |import app.cash.sqldelight.db.SqlSchema
      |import com.example.DataQueries
      |import com.example.Data_
      |import com.example.Other
      |import com.example.TestDatabase
      |import kotlin.Int
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlSchema
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
      |) : SuspendingTransacterImpl(driver), TestDatabase {
      |  public override val dataQueries: DataQueries = DataQueries(driver, data_Adapter, otherAdapter)
      |
      |  public object Schema : SqlSchema {
      |    public override val version: Int
      |      get() = 1
      |
      |    public override fun create(driver: SqlDriver): QueryResult<Unit> = QueryResult.AsyncValue {
      |      driver.execute(null, ""${'"'}
      |          |CREATE TABLE data (
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0).await()
      |      driver.execute(null, ""${'"'}
      |          |CREATE TABLE other (
      |          |  id INTEGER PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0).await()
      |    }
      |
      |    public override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Int,
      |      newVersion: Int,
      |    ): QueryResult<Unit> = QueryResult.AsyncValue {
      |    }
      |  }
      |}
      |
      """.trimMargin(),
    )

    val dataQueries = File(result.outputDirectory, "com/example/DataQueries.kt")
    Truth.assertThat(result.compilerOutput).containsKey(dataQueries)
    Truth.assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.SuspendingTransacterImpl
      |import app.cash.sqldelight.db.QueryResult
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
      |  driver: SqlDriver,
      |  private val data_Adapter: Data_.Adapter,
      |  private val otherAdapter: Other.Adapter,
      |) : SuspendingTransacterImpl(driver) {
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
      |  public suspend fun insertData(id: Long?, value_: List?): Unit {
      |    driver.execute(${insert.id}, ""${'"'}
      |        |INSERT INTO data
      |        |VALUES (?, ?)
      |        ""${'"'}.trimMargin(), 2) {
      |          bindLong(0, id)
      |          bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |        }.await()
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
      |    public override fun <R> execute(mapper: (SqlCursor) -> R): QueryResult<R> =
      |        driver.executeQuery(${select.id}, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id = ?
      |    ""${'"'}.trimMargin(), mapper, 1) {
      |      bindLong(0, id)
      |    }
      |
      |    public override fun toString(): String = "Data.sq:selectForId"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }
}
