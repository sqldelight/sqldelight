package com.squareup.sqldelight.drivers.sqljs

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.sqljs.initSqlDriver
import co.touchlab.stately.concurrency.AtomicInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsQueryTest {

  private val mapper = { cursor: SqlCursor ->
    TestData(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
    )
  }

  private val schema = object : SqlSchema<QueryResult.Value<Unit>> {
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
  }

  private fun testing(action: suspend CoroutineScope.(SqlDriver) -> Unit) = runTest {
    val driver = initSqlDriver().await()
    schema.create(driver)
    action(driver)
    driver.close()
  }

  @Test fun executeAsOne() = testing { driver ->

    val data1 = TestData(1, "val1")
    driver.insertTestData(data1)

    assertEquals(data1, driver.testDataQuery().executeAsOne())
  }

  @Test fun executeAsOneTwoTimes() = testing { driver ->

    val data1 = TestData(1, "val1")
    driver.insertTestData(data1)

    val query = driver.testDataQuery()

    assertEquals(query.executeAsOne(), query.executeAsOne())
  }

  @Test fun executeAsOneThrowsNpeForNoRows() = testing { driver ->
    assertFailsWith<NullPointerException> {
      driver.testDataQuery().executeAsOne()
    }
  }

  @Test fun executeAsOneThrowsIllegalStateExceptionForManyRows() = testing { driver ->
    assertFailsWith<IllegalStateException> {
      driver.insertTestData(TestData(1, "val1"))
      driver.insertTestData(TestData(2, "val2"))

      driver.testDataQuery().executeAsOne()
    }
  }

  @Test fun executeAsOneOrNull() = testing { driver ->

    val data1 = TestData(1, "val1")
    driver.insertTestData(data1)

    val query = driver.testDataQuery()
    assertEquals(data1, query.executeAsOneOrNull())
  }

  @Test fun executeAsOneOrNullReturnsNullForNoRows() = testing { driver ->
    assertNull(driver.testDataQuery().executeAsOneOrNull())
  }

  @Test fun executeAsOneOrNullThrowsIllegalStateExceptionForManyRows() = testing { driver ->
    assertFailsWith<IllegalStateException> {
      driver.insertTestData(TestData(1, "val1"))
      driver.insertTestData(TestData(2, "val2"))

      driver.testDataQuery().executeAsOneOrNull()
    }
  }

  @Test fun executeAsList() = testing { driver ->

    val data1 = TestData(1, "val1")
    val data2 = TestData(2, "val2")

    driver.insertTestData(data1)
    driver.insertTestData(data2)

    assertEquals(listOf(data1, data2), driver.testDataQuery().executeAsList())
  }

  @Test fun executeAsListForNoRows() = testing { driver ->
    assertTrue(driver.testDataQuery().executeAsList().isEmpty())
  }

  @Test fun notifyDataChangedNotifiesListeners() = testing { driver ->

    val notifies = AtomicInt(0)
    val query = driver.testDataQuery()
    val listener = object : Query.Listener {
      override fun queryResultsChanged() {
        notifies.incrementAndGet()
      }
    }

    query.addListener(listener)
    assertEquals(0, notifies.get())

    driver.notifyListeners(arrayOf("test"))
    assertEquals(1, notifies.get())
  }

  @Test fun removeListenerActuallyRemovesListener() = testing { driver ->

    val notifies = AtomicInt(0)
    val query = driver.testDataQuery()
    val listener = object : Query.Listener {
      override fun queryResultsChanged() {
        notifies.incrementAndGet()
      }
    }

    query.addListener(listener)
    query.removeListener(listener)
    driver.notifyListeners(arrayOf("test"))
    assertEquals(0, notifies.get())
  }

  private fun SqlDriver.insertTestData(testData: TestData) {
    execute(1, "INSERT INTO test VALUES (?, ?)", 2) {
      bindLong(0, testData.id)
      bindString(1, testData.value)
    }
  }

  private fun SqlDriver.testDataQuery(): Query<TestData> {
    return object : Query<TestData>(mapper) {
      override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
        return executeQuery(0, "SELECT * FROM test", mapper, 0, null)
      }

      override fun addListener(listener: Listener) {
        addListener(listener, arrayOf("test"))
      }

      override fun removeListener(listener: Listener) {
        removeListener(listener, arrayOf("test"))
      }
    }
  }

  private data class TestData(val id: Long, val value: String)
}
