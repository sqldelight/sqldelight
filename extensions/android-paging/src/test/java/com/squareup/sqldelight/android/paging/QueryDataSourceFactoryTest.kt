package com.squareup.sqldelight.android.paging

import androidx.paging.PositionalDataSource.LoadInitialCallback
import androidx.paging.PositionalDataSource.LoadInitialParams
import androidx.paging.PositionalDataSource.LoadRangeCallback
import androidx.paging.PositionalDataSource.LoadRangeParams
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.android.AndroidSqlDatabase
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabase.Schema
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXECUTE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class QueryDataSourceFactoryTest {
  private lateinit var database: SqlDatabase

  @Before
  fun before() {
    database = AndroidSqlDatabase(object : Schema {
      override val version: Int = 1

      override fun create(db: SqlDatabase) {
        db.prepareStatement(null, "CREATE TABLE testTable (value INTEGER PRIMARY KEY)", EXECUTE, 0)
            .execute()

        for (i in 0L..100L) {
          insert(i, db)
        }
      }

      override fun migrate(
        db: SqlDatabase,
        oldVersion: Int,
        newVersion: Int
      ) {
        throw AssertionError("DB Migration shouldn't occur")
      }
    }, RuntimeEnvironment.application)
  }

  @Test
  fun `initial load gives expected results back`() {
    val dataSource = QueryDataSourceFactory(
        queryProvider = ::queryFor,
        countQuery = countQuery()
    ).create()

    lateinit var data: MutableList<Long>

    dataSource.loadInitial(LoadInitialParams(0, 10, 10, true), loadInitial { data = it })
    assertThat(data).containsExactlyElementsIn(0L..9L).inOrder()
  }

  @Test
  fun `loadRange gives expected results back`() {
    val dataSource = QueryDataSourceFactory(
        queryProvider = ::queryFor,
        countQuery = countQuery()
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
        countQuery = countQuery()
    ).create()

    dataSource.addInvalidatedCallback {
      invalidated++
    }

    lateinit var data: MutableList<Long>

    dataSource.loadRange(LoadRangeParams(95, 10), loadRange { data = it })
    assertThat(data).containsExactlyElementsIn(95L..100L).inOrder()

    currentQuery.notifyDataChanged()
    assertThat(invalidated).isEqualTo(1)
  }

  private fun countQuery() =
    Query(2, mutableListOf(), database, "SELECT count(*) FROM testTable", { it.getLong(0)!! })

  private fun insert(value: Long, db: SqlDatabase = database) {
    val insert = db.prepareStatement(0, "INSERT INTO testTable (value) VALUES (?)", INSERT, 1)
    insert.bindLong(1, value)
    insert.execute()
  }

  private fun queryFor(
    limit: Long,
    offset: Long
  ): Query<Long> {
    return object : Query<Long>(
        mutableListOf(),
        { cursor -> cursor.getLong(0)!! }
    ) {
      override fun createStatement(): SqlPreparedStatement {
        val statement =
          database.prepareStatement(1, "SELECT value FROM testTable LIMIT ? OFFSET ?", SELECT, 2)
        statement.bindLong(1, limit)
        statement.bindLong(2, offset)
        return statement
      }
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