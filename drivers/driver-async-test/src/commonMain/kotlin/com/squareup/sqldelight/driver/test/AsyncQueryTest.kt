package com.squareup.sqldelight.driver.test

import app.cash.sqldelight.async.AsyncQuery
import app.cash.sqldelight.async.db.AsyncSqlCursor
import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.internal.Atomic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class AsyncQueryTest : AsyncTestBase() {
  private val mapper = { cursor: AsyncSqlCursor ->
    TestData(
      cursor.getLong(0)!!, cursor.getString(1)!!
    )
  }

  private lateinit var driver: AsyncSqlDriver

  override suspend fun setup() {
    driver = setupDatabase(
      schema = object : AsyncSqlDriver.Schema {
        override val version: Int = 1

        override suspend fun create(driver: AsyncSqlDriver) {
          driver.execute(
            null,
            """
              CREATE TABLE test (
                id INTEGER NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
               );
            """.trimIndent(),
            0
          )
        }

        override suspend fun migrate(
          driver: AsyncSqlDriver,
          oldVersion: Int,
          newVersion: Int
        ) {
          // No-op.
        }
      }
    )
  }

  override suspend fun teardown() {
    driver.close()
  }

  @Test fun executeAsOne() = runTest {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    assertEquals(data1, testDataQuery().executeAsOne())
  }

  @Test fun executeAsOneTwoTimes() = runTest {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    val query = testDataQuery()

    assertEquals(query.executeAsOne(), query.executeAsOne())
  }

  @Test fun executeAsOneThrowsNpeForNoRows() = runTest {
    try {
      testDataQuery().executeAsOne()
      throw AssertionError("Expected an IllegalStateException")
    } catch (ignored: NullPointerException) {
    }
  }

  @Test fun executeAsOneThrowsIllegalStateExceptionForManyRows() = runTest {
    try {
      insertTestData(TestData(1, "val1"))
      insertTestData(TestData(2, "val2"))

      testDataQuery().executeAsOne()
      throw AssertionError("Expected an IllegalStateException")
    } catch (ignored: IllegalStateException) {
    }
  }

  @Test fun executeAsOneOrNull() = runTest {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    val query = testDataQuery()
    assertEquals(data1, query.executeAsOneOrNull())
  }

  @Test fun executeAsOneOrNullReturnsNullForNoRows() = runTest {
    assertNull(testDataQuery().executeAsOneOrNull())
  }

  @Test fun executeAsOneOrNullThrowsIllegalStateExceptionForManyRows() = runTest {
    try {
      insertTestData(TestData(1, "val1"))
      insertTestData(TestData(2, "val2"))

      testDataQuery().executeAsOneOrNull()
      throw AssertionError("Expected an IllegalStateException")
    } catch (ignored: IllegalStateException) {
    }
  }

  @Test fun executeAsList() = runTest {
    val data1 = TestData(1, "val1")
    val data2 = TestData(2, "val2")

    insertTestData(data1)
    insertTestData(data2)

    assertEquals(listOf(data1, data2), testDataQuery().executeAsList())
  }

  @Test fun executeAsListForNoRows() = runTest {
    assertTrue(testDataQuery().executeAsList().isEmpty())
  }

  @Test fun notifyDataChangedNotifiesListeners() = runTest {
    val notifies = Atomic(0)
    val query = testDataQuery()
    val listener = object : AsyncQuery.Listener {
      override fun queryResultsChanged() {
        notifies.increment()
      }
    }

    query.addListener(listener)
    assertEquals(0, notifies.get())

    driver.notifyListeners(arrayOf("test"))
    assertEquals(1, notifies.get())
  }

  @Test fun removeListenerActuallyRemovesListener() {
    val notifies = Atomic(0)
    val query = testDataQuery()
    val listener = object : AsyncQuery.Listener {
      override fun queryResultsChanged() {
        notifies.increment()
      }
    }

    query.addListener(listener)
    query.removeListener(listener)
    driver.notifyListeners(arrayOf("test"))
    assertEquals(0, notifies.get())
  }

  private fun insertTestData(testData: TestData) = runTest {
    driver.execute(1, "INSERT INTO test VALUES (?, ?)", 2) {
      bindLong(1, testData.id)
      bindString(2, testData.value)
    }
  }

  private fun testDataQuery(): AsyncQuery<TestData> {
    return object : AsyncQuery<TestData>(mapper) {
      override suspend fun <R> execute(mapper: (AsyncSqlCursor) -> R): R {
        return driver.executeQuery(0, "SELECT * FROM test", mapper, 0, null)
      }

      override fun addListener(listener: Listener) {
        driver.addListener(listener, arrayOf("test"))
      }

      override fun removeListener(listener: Listener) {
        driver.removeListener(listener, arrayOf("test"))
      }
    }
  }

  private data class TestData(val id: Long, val value: String)
}

// Not actually atomic, the type needs to be as the listeners get frozen.
private fun Atomic<Int>.increment() = set(get() + 1)
