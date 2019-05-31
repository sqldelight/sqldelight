package com.squareup.sqldelight.core

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
      |import kotlin.collections.List
      |import kotlin.collections.MutableList
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
      |    get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver, dataAdapter: Data.Adapter):
      |        TestDatabase = TestDatabaseImpl(driver, dataAdapter)
      |
      |private class TestDatabaseImpl(driver: SqlDriver, internal val dataAdapter: Data.Adapter) :
      |        TransacterImpl(driver), TestDatabase {
      |    override val dataQueries: DataQueriesImpl = DataQueriesImpl(this, driver)
      |
      |    object Schema : SqlDriver.Schema {
      |        override val version: Int
      |            get() = 1
      |
      |        override fun create(driver: SqlDriver) {
      |            driver.execute(null, ""${'"'}
      |                    |CREATE TABLE data (
      |                    |  id INTEGER PRIMARY KEY,
      |                    |  value TEXT
      |                    |)
      |                    ""${'"'}.trimMargin(), 0)
      |        }
      |
      |        override fun migrate(
      |            driver: SqlDriver,
      |            oldVersion: Int,
      |            newVersion: Int
      |        ) {
      |        }
      |    }
      |}
      |
      |private class DataQueriesImpl(private val database: TestDatabaseImpl, private val driver: SqlDriver)
      |        : TransacterImpl(driver), DataQueries {
      |    internal val selectForId: MutableList<Query<*>> = copyOnWriteList()
      |
      |    override fun <T : Any> selectForId(id: Long, mapper: (id: Long, value: List?) -> T): Query<T> =
      |            SelectForId(id) { cursor ->
      |        mapper(
      |            cursor.getLong(0)!!,
      |            cursor.getString(1)?.let(database.dataAdapter.valueAdapter::decode)
      |        )
      |    }
      |
      |    override fun selectForId(id: Long): Query<Data> = selectForId(id, Data::Impl)
      |
      |    override fun insertData(id: Long?, value: List?) {
      |        driver.execute(Fixture1, ""${'"'}
      |        |INSERT INTO data
      |        |VALUES (?1, ?2)
      |        ""${'"'}.trimMargin(), 2) {
      |            bindLong(1, id)
      |            bindString(2, if (value == null) null else
      |                    database.dataAdapter.valueAdapter.encode(value))
      |        }
      |        notifyQueries(database.dataQueries.selectForId)
      |    }
      |
      |    private inner class SelectForId<out T : Any>(private val id: Long, mapper: (SqlCursor) -> T) :
      |            Query<T>(selectForId, mapper) {
      |        override fun execute(): SqlCursor = driver.executeQuery(Fixture0, ""${'"'}
      |        |SELECT *
      |        |FROM data
      |        |WHERE id = ?1
      |        ""${'"'}.trimMargin(), 1) {
      |            bindLong(1, id)
      |        }
      |    }
      |}
      |""".trimMargin())
  }
}

