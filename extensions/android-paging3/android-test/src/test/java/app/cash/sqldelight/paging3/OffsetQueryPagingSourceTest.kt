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

import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.recyclerview.widget.DiffUtil
import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class OffsetQueryPagingSourceTest {

  private lateinit var driver: SqlDriver
  private lateinit var transacter: Transacter

  @Before
  fun init() {
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    driver.execute(null, "CREATE TABLE TestItem(id INTEGER NOT NULL PRIMARY KEY);", emptyList())
    transacter = object : TransacterImpl(driver) {}
  }

  @Test
  fun test_itemCount() = runTest {
    insertItems(ITEMS_LIST)

    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    pagingSource.refresh()

    Pager(CONFIG, pagingSourceFactory = { pagingSource })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertThat(itemCount).isEqualTo(100)
      }
  }

  @Test
  fun invalidDbQuery_pagingSourceDoesNotInvalidate() = runTest {
    insertItems(ITEMS_LIST)
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    // load once to register db observers
    pagingSource.refresh()
    assertThat(pagingSource.invalid).isFalse()

    val result = deleteItem(TestItem(1000))

    // invalid delete. Should have 0 items deleted and paging source remains valid
    assertThat(result).isEqualTo(0)
    assertThat(pagingSource.invalid).isFalse()
  }

  @Test
  fun load_initialLoad() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    val result = pagingSource.refresh() as LoadResult.Page

    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(0, 15))
  }

  @Test
  fun load_initialEmptyLoad() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    val result = pagingSource.refresh() as LoadResult.Page

    assertTrue(result.data.isEmpty())

    // now add items
    insertItems(ITEMS_LIST)

    // invalidate pagingSource to imitate invalidation from running refreshVersionSync
    pagingSource.invalidate()
    assertTrue(pagingSource.invalid)

    // this refresh should check pagingSource's invalid status, realize it is invalid, and
    // return a LoadResult.Invalid
    assertThat(pagingSource.refresh()).isInstanceOf(LoadResult.Invalid::class.java)
  }

  @Test
  fun load_initialLoadWithInitialKey() = runTest {
    insertItems(ITEMS_LIST)
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    // refresh with initial key = 20
    val result = pagingSource.refresh(key = 20) as LoadResult.Page

    // item in pos 21-35 (TestItemId 20-34) loaded
    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(20, 35))
  }

  @Test
  fun invalidInitialKey_dbEmpty_returnsEmpty() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    val result = pagingSource.refresh(key = 101) as LoadResult.Page

    assertThat(result.data).isEmpty()
  }

  @Test
  fun invalidInitialKey_keyTooLarge_returnsLastPage() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    val result = pagingSource.refresh(key = 101) as LoadResult.Page

    // should load the last page
    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(85, 100))
  }

  @Test
  fun invalidInitialKey_negativeKey() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    // should throw error when initial key is negative
    val expectedException = assertFailsWith<IllegalArgumentException> {
      pagingSource.refresh(key = -1)
    }
    // default message from Paging 3 for negative initial key
    assertThat(expectedException.message).isEqualTo("itemsBefore cannot be negative")
  }

  @Test
  fun append_middleOfList() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    val result = pagingSource.append(key = 20) as LoadResult.Page

    // item in pos 21-25 (TestItemId 20-24) loaded
    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(20, 25))
    assertThat(result.nextKey).isEqualTo(25)
    assertThat(result.prevKey).isEqualTo(20)
  }

  @Test
  fun append_availableItemsLessThanLoadSize() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    val result = pagingSource.append(key = 97) as LoadResult.Page

    // item in pos 98-100 (TestItemId 97-99) loaded
    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(97, 100))
    assertThat(result.nextKey).isNull()
    assertThat(result.prevKey).isEqualTo(97)
  }

  @Test
  fun load_consecutiveAppend() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    // first append
    val result = pagingSource.append(key = 30) as LoadResult.Page

    // TestItemId 30-34 loaded
    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(30, 35))
    // second append using nextKey from previous load
    val result2 = pagingSource.append(key = result.nextKey) as LoadResult.Page

    // TestItemId 35 - 39 loaded
    assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(35, 40))
  }

  @Test
  fun append_invalidResult() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    // first append
    val result = pagingSource.append(key = 30) as LoadResult.Page

    // TestItemId 30-34 loaded
    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(30, 35))

    // invalidate pagingSource to imitate invalidation from running refreshVersionSync
    pagingSource.invalidate()

    // this append should check pagingSource's invalid status, realize it is invalid, and
    // return a LoadResult.Invalid
    val result2 = pagingSource.append(key = result.nextKey)

    assertThat(result2).isInstanceOf(LoadResult.Invalid::class.java)
  }

  @Test
  fun prepend_middleOfList() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    val result = pagingSource.prepend(key = 30) as LoadResult.Page

    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(25, 30))
    assertThat(result.nextKey).isEqualTo(30)
    assertThat(result.prevKey).isEqualTo(25)
  }

  @Test
  fun prepend_availableItemsLessThanLoadSize() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    val result = pagingSource.prepend(key = 3) as LoadResult.Page

    // items in pos 0 - 2 (TestItemId 0 - 2) loaded
    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(0, 3))
    assertThat(result.nextKey).isEqualTo(3)
    assertThat(result.prevKey).isNull()
  }

  @Test
  fun load_consecutivePrepend() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    // first prepend
    val result = pagingSource.prepend(key = 20) as LoadResult.Page

    // items pos 16-20 (TestItemId 15-19) loaded
    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))
    // second prepend using prevKey from previous load
    val result2 = pagingSource.prepend(key = result.prevKey) as LoadResult.Page

    // items pos 11-15 (TestItemId 10 - 14) loaded
    assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(10, 15))
  }

  @Test
  fun prepend_invalidResult() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    // first prepend
    val result = pagingSource.prepend(key = 20) as LoadResult.Page

    // items pos 16-20 (TestItemId 15-19) loaded
    assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))

    // invalidate pagingSource to imitate invalidation from running refreshVersionSync
    pagingSource.invalidate()

    // this prepend should check pagingSource's invalid status, realize it is invalid, and
    // return LoadResult.Invalid
    val result2 = pagingSource.prepend(key = result.prevKey)

    assertThat(result2).isInstanceOf(LoadResult.Invalid::class.java)
  }

  @Test
  fun test_itemsBefore() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    // for initial load
    val result = pagingSource.refresh(key = 50) as LoadResult.Page

    // initial loads items in pos 51 - 65, should have 50 items before
    assertThat(result.itemsBefore).isEqualTo(50)

    // prepend from initial load
    val result2 = pagingSource.prepend(key = result.prevKey) as LoadResult.Page

    // prepend loads items in pos 46 - 50, should have 45 item before
    assertThat(result2.itemsBefore).isEqualTo(45)

    // append from initial load
    val result3 = pagingSource.append(key = result.nextKey) as LoadResult.Page

    // append loads items in position 66 - 70 , should have 65 item before
    assertThat(result3.itemsBefore).isEqualTo(65)
  }

  @Test
  fun test_itemsAfter() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    // for initial load
    val result = pagingSource.refresh(key = 30) as LoadResult.Page

    // initial loads items in position 31 - 45, should have 55 items after
    assertThat(result.itemsAfter).isEqualTo(55)

    // prepend from initial load
    val result2 = pagingSource.prepend(key = result.prevKey) as LoadResult.Page

    // prepend loads items in position 26 - 30, should have 70 item after
    assertThat(result2.itemsAfter).isEqualTo(70)

    // append from initial load
    val result3 = pagingSource.append(result.nextKey) as LoadResult.Page

    // append loads items in position 46 - 50 , should have 50 item after
    assertThat(result3.itemsAfter).isEqualTo(50)
  }

  @Test
  fun test_getRefreshKey() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    // initial load
    val result = pagingSource.refresh() as LoadResult.Page
    // 15 items loaded, assuming anchorPosition = 14 as the last item loaded
    var refreshKey = pagingSource.getRefreshKey(
      PagingState(
        pages = listOf(result),
        anchorPosition = 14,
        config = CONFIG,
        leadingPlaceholderCount = 0,
      ),
    )
    // should load around anchor position
    // Initial load size = 15, refresh key should be (15/2 = 7) items
    // before anchorPosition (14 - 7 = 7)
    assertThat(refreshKey).isEqualTo(7)

    // append after refresh
    val result2 = pagingSource.append(key = result.nextKey) as LoadResult.Page

    assertThat(result2.data).isEqualTo(ITEMS_LIST.subList(15, 20))
    refreshKey = pagingSource.getRefreshKey(
      PagingState(
        pages = listOf(result, result2),
        // 20 items loaded, assume anchorPosition = 19 as the last item loaded
        anchorPosition = 19,
        config = CONFIG,
        leadingPlaceholderCount = 0,
      ),
    )
    // initial load size 15. Refresh key should be (15/2 = 7) items before anchorPosition
    // (19 - 7 = 12)
    assertThat(refreshKey).isEqualTo(12)
  }

  @Test
  fun load_refreshKeyGreaterThanItemCount_lastPage() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    pagingSource.refresh(key = 70)

    deleteItems(40..100)

    // assume user was viewing last item of the refresh load with anchorPosition = 85,
    // initialLoadSize = 15. This mimics how getRefreshKey() calculates refresh key.
    val refreshKey = 85 - (15 / 2)
    assertThat(refreshKey).isEqualTo(78)

    val pagingSource2 = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    val result2 = pagingSource2.refresh(key = refreshKey) as LoadResult.Page

    // database should only have 40 items left. Refresh key is invalid at this point
    // (greater than item count after deletion)
    Pager(CONFIG, pagingSourceFactory = { pagingSource2 })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertThat(itemCount).isEqualTo(40)
      }
    // ensure that paging source can handle invalid refresh key properly
    // should load last page with items 25 - 40
    assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(25, 40))

    // should account for updated item count to return correct itemsBefore, itemsAfter,
    // prevKey, nextKey
    assertThat(result2.itemsBefore).isEqualTo(25)
    assertThat(result2.itemsAfter).isEqualTo(0)
    // no append can be triggered
    assertThat(result2.prevKey).isEqualTo(25)
    assertThat(result2.nextKey).isNull()
  }

  /**
   * Tests the behavior if user was viewing items in the top of the database and those items
   * were deleted.
   *
   * Currently, if anchorPosition is small enough (within bounds of 0 to loadSize/2), then on
   * invalidation from dropped items at the top, refresh will load with offset = 0. If
   * anchorPosition is larger than loadsize/2, then the refresh load's offset will
   * be 0 to (anchorPosition - loadSize/2).
   *
   * Ideally, in the future Paging will be able to handle this case better.
   */
  @Test
  fun load_refreshKeyGreaterThanItemCount_firstPage() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    pagingSource.refresh()

    Pager(CONFIG, pagingSourceFactory = { pagingSource })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertThat(itemCount).isEqualTo(100)
      }

    // items id 0 - 29 deleted (30 items removed)
    deleteItems(0..29)

    val pagingSource2 = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    // assume user was viewing first few items with anchorPosition = 0 and refresh key
    // clips to 0
    val refreshKey = 0

    val result2 = pagingSource2.refresh(key = refreshKey) as LoadResult.Page

    // database should only have 70 items left
    Pager(CONFIG, pagingSourceFactory = { pagingSource2 })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertThat(itemCount).isEqualTo(70)
      }
    // first 30 items deleted, refresh should load starting from pos 31 (item id 30 - 45)
    assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(30, 45))

    // should account for updated item count to return correct itemsBefore, itemsAfter,
    // prevKey, nextKey
    assertThat(result2.itemsBefore).isEqualTo(0)
    assertThat(result2.itemsAfter).isEqualTo(55)
    // no prepend can be triggered
    assertThat(result2.prevKey).isNull()
    assertThat(result2.nextKey).isEqualTo(15)
  }

  @Test
  fun load_loadSizeAndRefreshKeyGreaterThanItemCount() = runTest {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    insertItems(ITEMS_LIST)
    pagingSource.refresh(key = 30)

    Pager(CONFIG, pagingSourceFactory = { pagingSource })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertThat(itemCount).isEqualTo(100)
      }
    // items id 0 - 94 deleted (95 items removed)
    deleteItems(0..94)

    val pagingSource2 = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    // assume user was viewing first few items with anchorPosition = 0 and refresh key
    // clips to 0
    val refreshKey = 0

    val result2 = pagingSource2.refresh(key = refreshKey) as LoadResult.Page

    // database should only have 5 items left
    Pager(CONFIG, pagingSourceFactory = { pagingSource2 })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertThat(itemCount).isEqualTo(5)
      }
    // only 5 items should be loaded with offset = 0
    assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(95, 100))

    // should recognize that this is a terminal load
    assertThat(result2.itemsBefore).isEqualTo(0)
    assertThat(result2.itemsAfter).isEqualTo(0)
    assertThat(result2.prevKey).isNull()
    assertThat(result2.nextKey).isNull()
  }

  @Test
  fun test_jumpSupport() {
    val pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    assertTrue(pagingSource.jumpingSupported)
  }

  @Test
  fun load_initialEmptyLoad_QueryPagingSourceLong() = runTest {
    val pagingSource = QueryPagingSource(
      countQueryLong(),
      transacter,
      EmptyCoroutineContext,
      ::queryLong,
    )
    val result = pagingSource.refresh() as LoadResult.Page

    assertTrue(result.data.isEmpty())

    // now add items
    insertItems(ITEMS_LIST)

    // invalidate pagingSource to imitate invalidation from running refreshVersionSync
    pagingSource.invalidate()
    assertTrue(pagingSource.invalid)

    // this refresh should check pagingSource's invalid status, realize it is invalid, and
    // return a LoadResult.Invalid
    assertThat(pagingSource.refresh()).isInstanceOf(LoadResult.Invalid::class.java)
  }

  private fun query(limit: Int, offset: Int) = queryLong(limit.toLong(), offset.toLong())

  private fun queryLong(limit: Long, offset: Long) = object : Query<TestItem>(
    { cursor ->
      TestItem(cursor.getLong(0)!!)
    },
  ) {
    override fun <R> execute(mapper: (SqlCursor) -> R) = driver.executeQuery(1, "SELECT id FROM TestItem LIMIT ? OFFSET ?", mapper, listOf(30, 39)) {
      bindLong(0, limit)
      bindLong(1, offset)
    }

    override fun addListener(listener: Listener) = driver.addListener(listener, arrayOf("TestItem"))
    override fun removeListener(listener: Listener) = driver.removeListener(listener, arrayOf("TestItem"))
  }

  private fun countQuery() = Query(
    2,
    arrayOf("TestItem"),
    driver,
    "Test.sq",
    "count",
    "SELECT count(*) FROM TestItem",
    { it.getLong(0)!!.toInt() },
  )

  private fun countQueryLong() = Query(
    2,
    arrayOf("TestItem"),
    driver,
    "Test.sq",
    "count",
    "SELECT count(*) FROM TestItem",
    { it.getLong(0)!! },
  )

  private fun insertItems(items: List<TestItem>) {
    items.forEach {
      driver.execute(0, "INSERT INTO TestItem (id) VALUES (?)", listOf(34)) {
        bindLong(0, it.id)
      }
    }
  }

  private fun deleteItem(item: TestItem): Long =
    driver
      .execute(0, "DELETE FROM TestItem WHERE id = ?;", listOf(32)) {
        bindLong(0, item.id)
      }
      .value

  private fun deleteItems(range: IntRange): Long =
    driver
      .execute(0, "DELETE FROM TestItem WHERE id >= ? AND id <= ?", listOf(33, 45)) {
        bindLong(0, range.first.toLong())
        bindLong(1, range.last.toLong())
      }
      .value
}

private val CONFIG = PagingConfig(
  pageSize = 5,
  enablePlaceholders = true,
  initialLoadSize = 15,
)

private val ITEMS_LIST = List(100) { TestItem(id = it.toLong()) }

private val testItemDiffCallback = object : DiffUtil.ItemCallback<TestItem>() {
  override fun areItemsTheSame(oldItem: TestItem, newItem: TestItem): Boolean = oldItem.id == newItem.id
  override fun areContentsTheSame(oldItem: TestItem, newItem: TestItem): Boolean = oldItem == newItem
}

data class TestItem(val id: Long)

private fun createLoadParam(loadType: LoadType, key: Int?): PagingSource.LoadParams<Int> = when (loadType) {
  LoadType.REFRESH -> PagingSource.LoadParams.Refresh(
    key = key,
    loadSize = CONFIG.initialLoadSize,
    placeholdersEnabled = CONFIG.enablePlaceholders,
  )

  LoadType.APPEND -> PagingSource.LoadParams.Append(
    key = key ?: -1,
    loadSize = CONFIG.pageSize,
    placeholdersEnabled = CONFIG.enablePlaceholders,
  )

  LoadType.PREPEND -> PagingSource.LoadParams.Prepend(
    key = key ?: -1,
    loadSize = CONFIG.pageSize,
    placeholdersEnabled = CONFIG.enablePlaceholders,
  )
}

private suspend fun PagingSource<Int, TestItem>.refresh(key: Int? = null): LoadResult<Int, TestItem> =
  load(createLoadParam(LoadType.REFRESH, key))

private suspend fun PagingSource<Int, TestItem>.append(key: Int?): LoadResult<Int, TestItem> =
  load(createLoadParam(LoadType.APPEND, key))

private suspend fun PagingSource<Int, TestItem>.prepend(key: Int?): LoadResult<Int, TestItem> =
  load(createLoadParam(LoadType.PREPEND, key))
