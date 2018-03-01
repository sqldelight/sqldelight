package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import com.squareup.sqldelight.sqlite.jdbc.SqliteJdbcOpenHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class QueryTest {
  private val mapper = { cursor: SqlResultSet -> TestData(cursor.getLong(0), cursor.getString(1)) }

  private lateinit var database: SqlDatabase
  private lateinit var connection: SqlDatabaseConnection
  private lateinit var insertTestData: SqlPreparedStatement

  @Before fun setup() {
    database = SqliteJdbcOpenHelper()
    connection = database.getConnection()

    connection.prepareStatement("""
        CREATE TABLE test (
          _id INTEGER NOT NULL PRIMARY KEY,
          value TEXT NOT NULL
        );
        """.trimIndent()).execute()

    insertTestData = connection.prepareStatement("INSERT INTO test VALUES (?, ?)")
  }

  @After fun tearDown() {
    database.close()
  }

  @Test fun executeAsOne() {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    assertThat(testDataQuery().executeAsOne()).isEqualTo(data1)
  }

  @Test fun executeAsOneThrowsNpeForNoRows() {
    try {
      testDataQuery().executeAsOne()
      throw AssertionError("Expected a NullPointerException")
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
    assertThat(query.executeAsOneOrNull()).isEqualTo(data1)
  }

  @Test fun executeAsOneOrNullReturnsNullForNoRows() {
    assertThat(testDataQuery().executeAsOneOrNull()).isNull()
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

    assertThat(testDataQuery().executeAsList()).containsExactly(data1, data2)
  }

  @Test fun executeAsListForNoRows() {
    assertThat(testDataQuery().executeAsList()).isEmpty()
  }

  @Test fun notifyResultSetChangedNotifiesListeners() {
    val notifies = AtomicInteger(0)
    val query = testDataQuery()
    val listener = object : Query.Listener {
      override fun queryResultsChanged() {
        notifies.incrementAndGet()
      }
    }

    query.addListener(listener)
    assertThat(notifies.get()).isEqualTo(0)

    query.notifyResultSetChanged()
    assertThat(notifies.get()).isEqualTo(1)
  }

  private fun insertTestData(testData: TestData) {
    insertTestData.bindLong(1, testData._id)
    insertTestData.bindString(2, testData.value)
    insertTestData.execute()
  }

  private fun testDataQuery(): Query<TestData> {
    val statement = connection.prepareStatement("SELECT * FROM test")
    return Query(statement, mutableListOf(), mapper)
  }

  private data class TestData(val _id: Long, val value: String)
}
