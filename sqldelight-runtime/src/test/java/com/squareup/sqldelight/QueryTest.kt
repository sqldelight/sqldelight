package com.squareup.sqldelight

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.arch.persistence.db.SupportSQLiteProgram
import android.arch.persistence.db.SupportSQLiteStatement
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory
import android.database.Cursor
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class QueryTest {
  private val callback = object : SupportSQLiteOpenHelper.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
      db.execSQL("""
        CREATE TABLE test (
          _id INTEGER NOT NULL PRIMARY KEY,
          value TEXT NOT NULL
        );
        """.trimIndent())
    }

    override fun onUpgrade(db: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) = Unit
  }

  private val configuration = SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.application)
      .callback(callback)
      .build()

  private val mapper = { cursor: Cursor -> TestData(cursor.getLong(0), cursor.getString(1)) }

  private lateinit var database: SupportSQLiteOpenHelper
  private lateinit var insertTestData: SupportSQLiteStatement

  @Before fun setup() {
    database = FrameworkSQLiteOpenHelperFactory()
        .create(configuration)
    insertTestData = database.writableDatabase.compileStatement("INSERT INTO test VALUES (?, ?)")
  }

  @Test fun executeAsOne() {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    assertThat(TestDataQuery().executeAsOne()).isEqualTo(data1)
  }

  @Test fun executeAsOneThrowsNpeForNoRows() {
    try {
      TestDataQuery().executeAsOne()
      throw AssertionError("Expected a NullPointerException")
    } catch (ignored: NullPointerException) {

    }
  }

  @Test fun executeAsOneThrowsIllegalStateExceptionForManyRows() {
    try {
      insertTestData(TestData(1, "val1"))
      insertTestData(TestData(2, "val2"))

      TestDataQuery().executeAsOne()
      throw AssertionError("Expected an IllegalStateException")
    } catch (ignored: IllegalStateException) {

    }
  }

  @Test fun executeAsOneOrNull() {
    val data1 = TestData(1, "val1")
    insertTestData(data1)

    val query = TestDataQuery()
    assertThat(query.executeAsOneOrNull()).isEqualTo(data1)
  }

  @Test fun executeAsOneOrNullReturnsNullForNoRows() {
    assertThat(TestDataQuery().executeAsOneOrNull()).isNull()
  }

  @Test fun executeAsOneOrNullThrowsIllegalStateExceptionForManyRows() {
    try {
      insertTestData(TestData(1, "val1"))
      insertTestData(TestData(2, "val2"))

      TestDataQuery().executeAsOneOrNull()
      throw AssertionError("Expected an IllegalStateException")
    } catch (ignored: IllegalStateException) {

    }
  }

  @Test fun executeAsList() {
    val data1 = TestData(1, "val1")
    val data2 = TestData(2, "val2")

    insertTestData(data1)
    insertTestData(data2)

    assertThat(TestDataQuery().executeAsList()).containsExactly(data1, data2)
  }

  @Test fun executeAsListForNoRows() {
    assertThat(TestDataQuery().executeAsList()).isEmpty()
  }

  @Test fun notifyResultSetChangedNotifiesListeners() {
    val notifies = AtomicInteger(0)
    val query = TestDataQuery()
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
    insertTestData.executeInsert()
  }

  private data class TestData(val _id: Long, val value: String)

  private inner class TestDataQuery: Query<TestData>(database, mutableListOf(), mapper) {
    override fun bindTo(statement: SupportSQLiteProgram?) = Unit
    override fun getSql() = "SELECT * FROM test"
  }
}
