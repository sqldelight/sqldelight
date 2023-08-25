package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.stately.concurrency.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeDriverConcurrencyTest {

  private val mapper = { cursor: SqlCursor ->
    TestData(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
    )
  }

  private lateinit var driver: SqlDriver

  private fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    val name = "testdb"
    DatabaseFileContext.deleteDatabase(name)
    return NativeSqliteDriver(schema, name)
  }

  @BeforeTest
  fun setup() {
    driver = setupDatabase(
      schema = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 1

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
          driver.execute(
            null,
            """
              CREATE TABLE test (
                id INTEGER NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
               );
            """.trimIndent(),
            0,
          )
          return QueryResult.Unit
        }

        override fun migrate(
          driver: SqlDriver,
          oldVersion: Long,
          newVersion: Long,
          vararg callbacks: AfterVersion,
        ): QueryResult.Value<Unit> {
          // No-op.
          return QueryResult.Unit
        }
      },
    )
  }

  @OptIn(ExperimentalStdlibApi::class)
  @Test
  fun basicConcurrencyCheckForListeners() {
    val notifies = AtomicInt(0)
    val query = testDataQuery()

    val workers = (0..<10).map { Worker.start() }

    val block = {
      query.addListener(Query.Listener { notifies.incrementAndGet() })
    }

    workers.forEach { w ->
      repeat(10) {
        w.execute(TransferMode.SAFE, { block }) { it() }
      }
    }

    val futures = workers.map { w -> w.requestTermination(processScheduledJobs = true) }

    futures.forEach { f -> f.result }

    assertEquals(0, notifies.get())

    driver.notifyListeners("test")
    assertEquals(100, notifies.get())
  }

  private fun testDataQuery(): Query<TestData> {
    return object : Query<TestData>(mapper) {
      override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
        return driver.executeQuery(0, "SELECT * FROM test", mapper, 0, null)
      }

      override fun addListener(listener: Listener) {
        driver.addListener("test", listener = listener)
      }

      override fun removeListener(listener: Listener) {
        driver.removeListener("test", listener = listener)
      }
    }
  }

  private data class TestData(val id: Long, val value: String)
}
