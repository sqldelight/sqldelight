package com.squareup.sqldelight.android.paging3

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class KeyedQueryPagingSourceTest {

  private lateinit var driver: SqlDriver
  private lateinit var transacter: Transacter

  @Before fun before() {
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    driver.execute(null, "CREATE TABLE testTable(value INTEGER PRIMARY KEY)", 0)
    (0L until 10L).forEach(this::insert)
    transacter = object : TransacterImpl(driver) {}
  }

  @Test fun `aligned page exhaustion gives correct results`() {
    val source = KeyedQueryPagingSource(
      queryProvider = this::query,
      pageBoundariesProvider = this::pageBoundaries,
      transacter = transacter,
      dispatcher = TestCoroutineDispatcher(),
    )

    runBlocking {
      val expected = (0L until 10L).chunked(2).iterator()
      var nextKey: Long? = null
      do {
        val results = source.load(Refresh(nextKey, 2, false))
        nextKey = (results as LoadResult.Page).nextKey
        assertEquals(expected = expected.next(), actual = results.data)
      } while (nextKey != null)
    }
  }

  @Test fun `misaligned page exhastion gives correct results`() {
    val source = KeyedQueryPagingSource(
      queryProvider = this::query,
      pageBoundariesProvider = this::pageBoundaries,
      transacter = transacter,
      dispatcher = TestCoroutineDispatcher(),
    )

    runBlocking {
      val expected = (0L until 10L).chunked(3).iterator()
      var nextKey: Long? = null
      do {
        val results = source.load(Refresh(nextKey, 3, false))
        nextKey = (results as LoadResult.Page).nextKey
        assertEquals(expected = expected.next(), actual = results.data)
      } while (nextKey != null)
    }
  }

  @Test fun `requesting a page with anchor not in step passes`() {
    val source = KeyedQueryPagingSource(
      queryProvider = this::query,
      pageBoundariesProvider = this::pageBoundaries,
      transacter = transacter,
      dispatcher = TestCoroutineDispatcher(),
    )

    val results = runBlocking { source.load(Refresh(key = 5L, loadSize = 2, false)) }

    assertEquals(listOf(5L), (results as LoadResult.Page).data)
  }

  @Test fun `misaligned last page has correct data`() {
    val source = KeyedQueryPagingSource(
      queryProvider = this::query,
      pageBoundariesProvider = this::pageBoundaries,
      transacter = transacter,
      dispatcher = TestCoroutineDispatcher(),
    )

    val results = runBlocking { source.load(Refresh(key = 9L, loadSize = 3, false)) }

    assertEquals(expected = listOf(9L), (results as LoadResult.Page).data)
    assertEquals(expected = 6L, results.prevKey)
    assertEquals(expected = null, results.nextKey)
  }

  @Test fun `invoking getRefreshKey before first load returns null key`() {
    val source = KeyedQueryPagingSource(
      queryProvider = this::query,
      pageBoundariesProvider = this::pageBoundaries,
      transacter = transacter,
      dispatcher = TestCoroutineDispatcher(),
    )

    assertNull(
      source.getRefreshKey(
        PagingState(
          emptyList(),
          null,
          PagingConfig(3),
          0
        )
      )
    )
  }

  @Test fun `invoking getRefreshKey with loaded first page returns correct result`() {
    val source = KeyedQueryPagingSource(
      queryProvider = this::query,
      pageBoundariesProvider = this::pageBoundaries,
      transacter = transacter,
      dispatcher = TestCoroutineDispatcher(),
    )

    val results = runBlocking { source.load(Refresh(key = null, loadSize = 3, false)) }
    val refreshKey = source.getRefreshKey(
      PagingState(
        listOf(results as LoadResult.Page),
        null,
        PagingConfig(3),
        0
      )
    )

    assertEquals(0L, refreshKey)
  }

  @Test fun `invoking getRefreshKey with single loaded middle page returns correct result`() {
    val source = KeyedQueryPagingSource(
      queryProvider = this::query,
      pageBoundariesProvider = this::pageBoundaries,
      transacter = transacter,
      dispatcher = TestCoroutineDispatcher(),
    )

    val results = runBlocking { source.load(Refresh(key = 6L, loadSize = 3, false)) }
    val refreshKey = source.getRefreshKey(
      PagingState(
        listOf(results as LoadResult.Page),
        null,
        PagingConfig(3),
        0
      )
    )

    assertEquals(6L, refreshKey)
  }

  private fun pageBoundaries(anchor: Long?, limit: Long): Query<Long> {
    val sql = """
      |SELECT value
      |FROM (
      |  SELECT
      |    value,
      |    CASE
      |      WHEN (row_number() OVER(ORDER BY value ASC) - 1) % ? = 0 THEN 1
      |      WHEN value = ? THEN 1
      |      ELSE 0
      |    END page_boundary
      |  FROM testTable
      |  ORDER BY value ASC
      |)
      |WHERE page_boundary = 1;
    """.trimMargin()

    return object : Query<Long>({ cursor -> cursor.getLong(0)!! }) {
      override fun execute() = driver.executeQuery(identifier = 3, sql = sql, parameters = 2) {
        bindLong(1, limit)
        bindLong(2, anchor)
      }

      override fun addListener(listener: Listener) = Unit
      override fun removeListener(listener: Listener) = Unit
    }
  }

  private fun query(beginInclusive: Long, endExclusive: Long?): Query<Long> {
    val sql = """
      |SELECT value FROM testTable
      |WHERE value >= :1 AND (value < :2 OR :2 IS NULL)
      |ORDER BY value ASC;
    """.trimMargin()

    return object : Query<Long>(
      { cursor -> cursor.getLong(0)!! }
    ) {
      override fun execute() = driver.executeQuery(identifier = 2, sql = sql, parameters = 2) {
        bindLong(1, beginInclusive)
        bindLong(2, endExclusive)
      }

      override fun addListener(listener: Listener) = Unit
      override fun removeListener(listener: Listener) = Unit
    }
  }

  private fun insert(value: Long, db: SqlDriver = driver) {
    db.execute(0, "INSERT INTO testTable (value) VALUES (?)", 1) {
      bindLong(1, value)
    }
  }
}
