/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.paging3

import app.cash.paging.PagingConfig
import app.cash.paging.PagingSourceLoadParamsRefresh
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.TransacterBase
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class KeyedQueryPagingSourceTest : BaseKeyedQueryPagingSourceTest() {
  override fun createTransacter(driver: SqlDriver): TransacterBase {
    return object : TransacterImpl(driver) {}
  }
}

@ExperimentalCoroutinesApi
class KeyedQueryPagingSourceWithSuspendingTransacterTest : BaseKeyedQueryPagingSourceTest() {
  override fun createTransacter(driver: SqlDriver): TransacterBase {
    return object : SuspendingTransacterImpl(driver) {}
  }
}

@ExperimentalCoroutinesApi
abstract class BaseKeyedQueryPagingSourceTest : DbTest {

  private lateinit var driver: SqlDriver
  private lateinit var transacter: TransacterBase
  private lateinit var source: KeyedQueryPagingSource<Long, Long>

  abstract fun createTransacter(driver: SqlDriver): TransacterBase

  override suspend fun setup(driver: SqlDriver) {
    this.driver = driver
    driver.execute(null, "CREATE TABLE testTable(value INTEGER PRIMARY KEY)", 0)
    (0L until 10L).forEach { this.insert(it) }
    transacter = createTransacter(driver)
    source = KeyedQueryPagingSource(
      queryProvider = this::query,
      pageBoundariesProvider = this::pageBoundaries,
      transacter = transacter,
      context = EmptyCoroutineContext,
    )
  }

  @Test fun aligned_page_exhaustion_gives_correct_results() = runDbTest {
    val expected = (0L until 10L).chunked(2).iterator()
    var nextKey: Long? = null
    do {
      val results = source.load(PagingSourceLoadParamsRefresh(nextKey, 2, false))
      nextKey = (results as PagingSourceLoadResultPage<Long, Long>).nextKey
      assertEquals(expected = expected.next(), actual = results.data)
    } while (nextKey != null)
  }

  @Test fun misaligned_page_exhaustion_gives_correct_results() = runDbTest {
    val expected = (0L until 10L).chunked(3).iterator()
    var nextKey: Long? = null
    do {
      val results = source.load(PagingSourceLoadParamsRefresh(nextKey, 3, false))
      nextKey = (results as PagingSourceLoadResultPage<Long, Long>).nextKey
      assertEquals(expected = expected.next(), actual = results.data)
    } while (nextKey != null)
  }

  @Test fun requesting_a_page_with_anchor_not_in_step_passes() = runDbTest {
    val results = source.load(PagingSourceLoadParamsRefresh(key = 5L, loadSize = 2, false))

    assertEquals(listOf(5L), (results as PagingSourceLoadResultPage<Long, Long>).data)
  }

  @Test fun misaligned_last_page_has_correct_data() = runDbTest {
    val results = source.load(PagingSourceLoadParamsRefresh(key = 9L, loadSize = 3, false))

    assertEquals(expected = listOf(9L), (results as PagingSourceLoadResultPage<Long, Long>).data)
    assertEquals(expected = 6L, results.prevKey)
    assertEquals(expected = null, results.nextKey)
  }

  @Test fun invoking_getRefreshKey_before_first_load_returns_null_key() = runDbTest {
    assertNull(
      source.getRefreshKey(
        PagingState(
          emptyList(),
          null,
          PagingConfig(3),
          0,
        ),
      ),
    )
  }

  @Test fun invoking_getRefreshKey_with_loaded_first_page_returns_correct_result() = runDbTest {
    val results = source.load(PagingSourceLoadParamsRefresh(key = null, loadSize = 3, false))
    val refreshKey = source.getRefreshKey(
      PagingState(
        listOf(results as PagingSourceLoadResultPage<Long, Long>),
        null,
        PagingConfig(3),
        0,
      ),
    )

    assertEquals(0L, refreshKey)
  }

  @Test fun invoking_getRefreshKey_with_single_loaded_middle_page_returns_correct_result() = runDbTest {
    val results = source.load(PagingSourceLoadParamsRefresh(key = 6L, loadSize = 3, false))
    val refreshKey = source.getRefreshKey(
      PagingState(
        listOf(results as PagingSourceLoadResultPage<Long, Long>),
        null,
        PagingConfig(3),
        0,
      ),
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
      override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>) = driver.executeQuery(identifier = 3, sql = sql, mapper = mapper, parameters = 2) {
        bindLong(0, limit)
        bindLong(1, anchor)
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
      { cursor -> cursor.getLong(0)!! },
    ) {
      override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>) = driver.executeQuery(identifier = 2, sql = sql, mapper = mapper, parameters = 2) {
        bindLong(0, beginInclusive)
        bindLong(1, endExclusive)
      }

      override fun addListener(listener: Listener) = Unit
      override fun removeListener(listener: Listener) = Unit
    }
  }

  private fun insert(value: Long, db: SqlDriver = driver) {
    db.execute(0, "INSERT INTO testTable (value) VALUES (?)", 1) {
      bindLong(0, value)
    }
  }
}
