package com.squareup.sqldelight

import com.squareup.sqldelight.Query.Listener
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXEC
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import com.squareup.sqldelight.db.SqlResultSet
import com.squareup.sqldelight.internal.QueryList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueryTest {
  private val mapper = { resultSet: SqlResultSet ->
    TestData(
        resultSet.getLong(0)!!, resultSet.getString(1)!!
    )
  }

  private lateinit var database: SqlDatabase
  private lateinit var connection: SqlDatabaseConnection
  private lateinit var insertTestData: SqlPreparedStatement

  @BeforeTest fun setup() {
    database = createSqlDatabase()
    connection = database.getConnection()

    connection.prepareStatement("""
        CREATE TABLE test (
          id INTEGER NOT NULL PRIMARY KEY,
          value TEXT NOT NULL
        );
        """.trimIndent(), EXEC, 0).execute()

    insertTestData = connection.prepareStatement("INSERT INTO test VALUES (?, ?)", INSERT, 2)
  }

  @AfterTest fun tearDown() {
    database.close()
  }

  @Test fun executeAsOne() {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    assertEquals(data1, testDataQuery().executeAsOne())
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

  @Test fun notifyResultSetChangedNotifiesListeners() {
    var notifies = 0
    val query = testDataQuery()
    val listener = object : Listener {
      override fun queryResultsChanged() {
        notifies++
      }
    }

    query.addListener(listener)
    assertEquals(0, notifies)

    query.notifyResultSetChanged()
    assertEquals(1, notifies)
  }

  private fun insertTestData(testData: TestData) {
    insertTestData.bindLong(1, testData.id)
    insertTestData.bindString(2, testData.value)
    insertTestData.execute()
  }

  private fun testDataQuery(): Query<TestData> {
    return object : Query<TestData>(QueryList(), mapper) {
      override fun createStatement(): SqlPreparedStatement {
        return connection.prepareStatement("SELECT * FROM test", SELECT, 0)
      }
    }
  }

  private data class TestData(val id: Long, val value: String)
}
