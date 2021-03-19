package com.squareup.sqldelight.android.paging3.rx3

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams.Refresh
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RxOffsetQueryPagingSourceTest {

  private lateinit var driver: SqlDriver
  private lateinit var transacter: Transacter

  @Before
  fun before() {
    driver = AndroidSqliteDriver(
      object : SqlDriver.Schema {
        override val version: Int = 1

        override fun create(db: SqlDriver) {
          db.execute(null, "CREATE TABLE testTable (value INTEGER PRIMARY KEY)", 0)

          for (i in 0L until 10L) {
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
      RuntimeEnvironment.application
    )
    transacter = object : TransacterImpl(driver) {}
  }

  @Test fun `RxPagingSource can sequentially exhast pages`() {
    val source = RxOffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      Schedulers.trampoline(),
    )

    val expected = (0L..10L).chunked(2).iterator()
    var nextKey: Long? = null
    do {
      val results = source.loadSingle(Refresh(nextKey, 2, false)).blockingGet()
      assertEquals(expected.next(), (results as PagingSource.LoadResult.Page).data)
      nextKey = results.nextKey
    } while (nextKey != null)
  }

  private fun query(limit: Long, offset: Long) = object : Query<Long>(
    mutableListOf(),
    { cursor -> cursor.getLong(0)!! }
  ) {
    override fun execute() = driver.executeQuery(1, "SELECT value FROM testTable LIMIT ? OFFSET ?", 2) {
      bindLong(1, limit)
      bindLong(2, offset)
    }
  }

  private fun countQuery() = Query(
    2,
    mutableListOf(),
    driver,
    "Test.sq",
    "count", "SELECT count(*) FROM testTable",
    { it.getLong(0)!! }
  )

  private fun insert(value: Long, db: SqlDriver = driver) {
    db.execute(0, "INSERT INTO testTable (value) VALUES (?)", 1) {
      bindLong(1, value)
    }
  }
}
