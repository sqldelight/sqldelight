package com.squareup.sqldelight.driver.test

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.internal.Atomic
import com.squareup.sqldelight.internal.copyOnWriteList
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

abstract class QueryTest {
  private val mapper = { cursor: SqlCursor ->
    TestData(
        cursor.getLong(0)!!, cursor.getString(1)!!
    )
  }

  private lateinit var database: SqlDatabase
  private lateinit var insertTestData: () -> SqlPreparedStatement

  abstract fun setupDatabase(schema: SqlDatabase.Schema): SqlDatabase

  @BeforeTest fun setup() {
    database = setupDatabase(
        schema = object : SqlDatabase.Schema {
          override val version: Int = 1

          override fun create(db: SqlDatabase) {
            db.prepareStatement(null, """
              CREATE TABLE test (
                id INTEGER NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
               );
               """.trimIndent(), SqlPreparedStatement.Type.EXECUTE, 0).execute()

          }

          override fun migrate(
            db: SqlDatabase,
            oldVersion: Int,
            newVersion: Int
          ) {
            // No-op.
          }
        }
    )

    insertTestData = {
      database.prepareStatement(1, "INSERT INTO test VALUES (?, ?)",
          SqlPreparedStatement.Type.INSERT, 2)
    }
  }

  @AfterTest fun tearDown() {
    database.close()
  }

  @Test fun executeAsOne() {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    assertEquals(data1, testDataQuery().executeAsOne())
  }

  @Test fun executeAsOneTwoTimes() {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    val query = testDataQuery()

    assertEquals(query.executeAsOne(), query.executeAsOne())
  }

  @Test fun executeAsOneThrowsNpeForNoRows() {
    try {
      testDataQuery().executeAsOne()
      throw AssertionError("Expected an IllegalStateException")
    } catch (ignored: NullPointerException) {

    }
  }

  @Test fun executeAsOneThrowsIllegalStateExceptionForManyRows() {
    try {
      insertTestData(TestData(1, "val1"))
      insertTestData(TestData(2, "val2"))

      testDataQuery().executeAsOne()
      throw AssertionError("Expected an IllegalStateException")
    } catch (ignored: IllegalStateException) {

    }
  }

  @Test fun executeAsOneOrNull() {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    val query = testDataQuery()
    assertEquals(data1, query.executeAsOneOrNull())
  }

  @Test fun executeAsOneOrNullReturnsNullForNoRows() {
    assertNull(testDataQuery().executeAsOneOrNull())
  }

  @Test fun executeAsOneOrNullThrowsIllegalStateExceptionForManyRows() {
    try {
      insertTestData(TestData(1, "val1"))
      insertTestData(TestData(2, "val2"))

      testDataQuery().executeAsOneOrNull()
      throw AssertionError("Expected an IllegalStateException")
    } catch (ignored: IllegalStateException) {

    }
  }

  @Test fun executeAsList() {
    val data1 = TestData(1, "val1")
    val data2 = TestData(2, "val2")

    insertTestData(data1)
    insertTestData(data2)

    assertEquals(listOf(data1, data2), testDataQuery().executeAsList())
  }

  @Test fun executeAsListForNoRows() {
    assertTrue(testDataQuery().executeAsList().isEmpty())
  }

  @Test fun notifyDataChangedNotifiesListeners() {
    val notifies = Atomic(0)
    val query = testDataQuery()
    val listener = object : Query.Listener {
      override fun queryResultsChanged() {
        notifies.increment()
      }
    }

    query.addListener(listener)
    assertEquals(0, notifies.get())

    query.notifyDataChanged()
    assertEquals(1, notifies.get())
  }

  @Test fun removeListenerActuallyRemovesListener() {
    val notifies = Atomic(0)
    val query = testDataQuery()
    val listener = object : Query.Listener {
      override fun queryResultsChanged() {
        notifies.increment()
      }
    }

    query.addListener(listener)
    query.removeListener(listener)
    query.notifyDataChanged()
    assertEquals(0, notifies.get())
  }

  private fun insertTestData(testData: TestData) {
    val insertTestData = insertTestData()
    insertTestData.bindLong(1, testData.id)
    insertTestData.bindString(2, testData.value)
    insertTestData.execute()
  }

  private fun testDataQuery(): Query<TestData> {
    return object : Query<TestData>(copyOnWriteList(), mapper) {
      override fun createStatement(): SqlPreparedStatement {
        return database.prepareStatement(0, "SELECT * FROM test",
            SqlPreparedStatement.Type.SELECT, 0)
      }
    }
  }

  private data class TestData(val id: Long, val value: String)
}

// Not actually atomic, the type needs to be as the listeners get frozen.
private fun Atomic<Int>.increment() = set(get() + 1)
