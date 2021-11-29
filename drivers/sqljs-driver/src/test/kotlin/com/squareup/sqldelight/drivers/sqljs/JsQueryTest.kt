package com.squareup.sqldelight.drivers.sqljs

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.Atomic
import kotlin.js.Promise
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsQueryTest {

  private val mapper = { cursor: SqlCursor ->
    TestData(
      cursor.getLong(0)!!, cursor.getString(1)!!
    )
  }

  private val schema = object : SqlDriver.Schema {
    override val version: Int = 1

    override fun create(driver: SqlDriver) {
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

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Int,
      newVersion: Int
    ) {
      // No-op.
    }
  }

  private lateinit var driverPromise: Promise<SqlDriver>

  @BeforeTest
  fun setup() {
    driverPromise = initSqlDriver().then {
      schema.create(it)
      it
    }
  }

  @AfterTest
  fun tearDown() {
    driverPromise.then { it.close() }
  }

  @Test fun executeAsOne() = driverPromise.then { driver ->

    val data1 = TestData(1, "val1")
    driver.insertTestData(data1)

    assertEquals(data1, driver.testDataQuery().executeAsOne())
  }

  @Test fun executeAsOneTwoTimes() = driverPromise.then { driver ->

    val data1 = TestData(1, "val1")
    driver.insertTestData(data1)

    val query = driver.testDataQuery()

    assertEquals(query.executeAsOne(), query.executeAsOne())
  }

  @Test fun executeAsOneThrowsNpeForNoRows() = driverPromise.then { driver ->
    assertFailsWith<NullPointerException> {
      driver.testDataQuery().executeAsOne()
    }
  }

  @Test fun executeAsOneThrowsIllegalStateExceptionForManyRows() = driverPromise.then { driver ->
    assertFailsWith<IllegalStateException> {
      driver.insertTestData(TestData(1, "val1"))
      driver.insertTestData(TestData(2, "val2"))

      driver.testDataQuery().executeAsOne()
    }
  }

  @Test fun executeAsOneOrNull() = driverPromise.then { driver ->

    val data1 = TestData(1, "val1")
    driver.insertTestData(data1)

    val query = driver.testDataQuery()
    assertEquals(data1, query.executeAsOneOrNull())
  }

  @Test fun executeAsOneOrNullReturnsNullForNoRows() = driverPromise.then { driver ->
    assertNull(driver.testDataQuery().executeAsOneOrNull())
  }

  @Test fun executeAsOneOrNullThrowsIllegalStateExceptionForManyRows() = driverPromise.then { driver ->
    assertFailsWith<IllegalStateException> {
      driver.insertTestData(TestData(1, "val1"))
      driver.insertTestData(TestData(2, "val2"))

      driver.testDataQuery().executeAsOneOrNull()
    }
  }

  @Test fun executeAsList() = driverPromise.then { driver ->

    val data1 = TestData(1, "val1")
    val data2 = TestData(2, "val2")

    driver.insertTestData(data1)
    driver.insertTestData(data2)

    assertEquals(listOf(data1, data2), driver.testDataQuery().executeAsList())
  }

  @Test fun executeAsListForNoRows() = driverPromise.then { driver ->
    assertTrue(driver.testDataQuery().executeAsList().isEmpty())
  }

  @Test fun notifyDataChangedNotifiesListeners() = driverPromise.then { driver ->

    val notifies = Atomic(0)
    val query = driver.testDataQuery()
    val listener = object : Query.Listener {
      override fun queryResultsChanged() {
        notifies.increment()
      }
    }

    query.addListener(listener)
    assertEquals(0, notifies.get())

    driver.notifyListeners("test")
    assertEquals(1, notifies.get())
  }

  @Test fun removeListenerActuallyRemovesListener() = driverPromise.then { driver ->

    val notifies = Atomic(0)
    val query = driver.testDataQuery()
    val listener = object : Query.Listener {
      override fun queryResultsChanged() {
        notifies.increment()
      }
    }

    query.addListener(listener)
    query.removeListener(listener)
    driver.notifyListeners("test")
    assertEquals(0, notifies.get())
  }

  private fun SqlDriver.insertTestData(testData: TestData) {
    execute(1, "INSERT INTO test VALUES (?, ?)", 2) {
      bindLong(1, testData.id)
      bindString(2, testData.value)
    }
  }

  private fun SqlDriver.testDataQuery(): Query<TestData> {
    return object : Query<TestData>(mapper) {
      override fun execute(): SqlCursor {
        return executeQuery(0, "SELECT * FROM test", 0)
      }

      override fun addListener(listener: Listener) {
        addListener(listener, "test")
      }

      override fun removeListener(listener: Listener) {
        removeListener(listener, "test")
      }
    }
  }

  private data class TestData(val id: Long, val value: String)
}

// Not actually atomic, the type needs to be as the listeners get frozen.
private fun Atomic<Int>.increment() = set(get() + 1)
