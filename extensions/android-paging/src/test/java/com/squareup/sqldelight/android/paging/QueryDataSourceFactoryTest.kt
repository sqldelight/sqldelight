package com.squareup.sqldelight.android.paging

import androidx.paging.PositionalDataSource.LoadInitialCallback
import androidx.paging.PositionalDataSource.LoadInitialParams
import androidx.paging.PositionalDataSource.LoadRangeCallback
import androidx.paging.PositionalDataSource.LoadRangeParams
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlDriver.Schema
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueryDataSourceFactoryTest {
  private lateinit var driver: SqlDriver
  private lateinit var transacter: Transacter

  @Before
  fun before() {
    driver = AndroidSqliteDriver(
      object : Schema {
        override val version: Int = 1

        override fun create(db: SqlDriver) {
          db.execute(null, "CREATE TABLE testTable (value INTEGER PRIMARY KEY)", 0)

          for (i in 0L..100L) {
            insert(i, db)
          }
        }

        override fun migrate(
          db: SqlDriver,
          oldVersion: Int,
          newVersion: Int
        ) {
          throw AssertionError("DB Migration shouldn't occur")
        }
      },
      getApplicationContext()
    )
    transacter = object : TransacterImpl(driver) {}
  }

  @Test
  fun `initial load gives expected results back`() {
    val dataSource = QueryDataSourceFactory(
      queryProvider = ::queryFor,
      countQuery = countQuery(),
      transacter = transacter
    ).create()

    lateinit var data: MutableList<Long>

    dataSource.loadInitial(LoadInitialParams(0, 10, 10, true), loadInitial { data = it })
    assertThat(data).containsExactlyElementsIn(0L..9L).inOrder()
  }

  @Test
  fun `loadRange gives expected results back`() {
    val dataSource = QueryDataSourceFactory(
      queryProvider = ::queryFor,
      countQuery = countQuery(),
      transacter = transacter
    ).create()

    lateinit var data: MutableList<Long>

    dataSource.loadRange(LoadRangeParams(10, 10), loadRange { data = it })
    assertThat(data).containsExactlyElementsIn(10L..19L).inOrder()
  }

  @Test
  fun `invalidating the backing query invalidates the data source`() {
    lateinit var currentQuery: Query<Long>

    var invalidated = 0
    val dataSource = QueryDataSourceFactory(
      queryProvider = provider@{ limit, offset ->
        currentQuery = queryFor(limit, offset)
        return@provider currentQuery
      },
      countQuery = countQuery(),
      transacter = transacter
    ).create()

    dataSource.addInvalidatedCallback {
      invalidated++
    }

    lateinit var data: MutableList<Long>

    dataSource.loadRange(LoadRangeParams(95, 10), loadRange { data = it })
    assertThat(data).containsExactlyElementsIn(95L..100L).inOrder()

    driver.notifyListeners(arrayOf("testTable"))
    assertThat(invalidated).isEqualTo(1)
  }

  private fun countQuery() =
    Query(2, arrayOf(), driver, "Test.sq", "count", "SELECT count(*) FROM testTable", { it.getLong(0)!! })

  private fun insert(value: Long, db: SqlDriver = driver) {
    db.execute(0, "INSERT INTO testTable (value) VALUES (?)", 1) {
      bindLong(1, value)
    }
  }

  private fun queryFor(
    limit: Long,
    offset: Long
  ): Query<Long> {
    return object : Query<Long>(
      { cursor -> cursor.getLong(0)!! }
    ) {
      override fun execute() = driver.executeQuery(1, "SELECT value FROM testTable LIMIT ? OFFSET ?", 2) {
        bindLong(1, limit)
        bindLong(2, offset)
      }

      override fun addListener(listener: Listener) = driver.addListener(listener, arrayOf("testTable"))
      override fun removeListener(listener: Listener) = driver.removeListener(listener, arrayOf("testTable"))
    }
  }

  private fun loadInitial(callback: (list: MutableList<Long>) -> Unit): LoadInitialCallback<Long> {
    return object : LoadInitialCallback<Long>() {
      override fun onResult(
        data: MutableList<Long>,
        position: Int,
        totalCount: Int
      ) {
        callback(data)
      }

      override fun onResult(
        data: MutableList<Long>,
        position: Int
      ) {
        throw AssertionError("Should always know count.")
      }
    }
  }

  private fun loadRange(callback: (list: MutableList<Long>) -> Unit): LoadRangeCallback<Long> {
    return object : LoadRangeCallback<Long>() {
      override fun onResult(data: MutableList<Long>) {
        callback(data)
      }
    }
  }
}
