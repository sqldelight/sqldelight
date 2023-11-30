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

import androidx.recyclerview.widget.DiffUtil
import app.cash.paging.LoadType
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadParamsAppend
import app.cash.paging.PagingSourceLoadParamsPrepend
import app.cash.paging.PagingSourceLoadParamsRefresh
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultInvalid
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first

@ExperimentalCoroutinesApi
class OffsetQueryPagingSourceTest : BaseOffsetQueryPagingSourceTest() {
  override fun createTransacter(driver: SqlDriver): TransacterBase {
    return object : TransacterImpl(driver) {}
  }
}

@ExperimentalCoroutinesApi
class OffsetQueryPagingSourceWithSuspendingTransacterTest : BaseOffsetQueryPagingSourceTest() {
  override fun createTransacter(driver: SqlDriver): TransacterBase {
    return object : SuspendingTransacterImpl(driver) {}
  }
}

@ExperimentalCoroutinesApi
abstract class BaseOffsetQueryPagingSourceTest : DbTest {

  private lateinit var driver: SqlDriver
  private lateinit var transacter: TransacterBase
  private lateinit var pagingSource: PagingSource<Int, TestItem>

  abstract fun createTransacter(driver: SqlDriver): TransacterBase

  override suspend fun setup(driver: SqlDriver) {
    this.driver = driver
    driver.execute(null, "CREATE TABLE TestItem(id INTEGER NOT NULL PRIMARY KEY);", 0)
    transacter = createTransacter(driver)
    pagingSource = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
  }

  @Test
  fun test_itemCount() = runDbTest {
    insertItems(ITEMS_LIST)

    pagingSource.refresh()

    Pager(CONFIG, pagingSourceFactory = { pagingSource })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertEquals(100, itemCount)
      }
  }

  @Test
  fun invalidDbQuery_pagingSourceDoesNotInvalidate() = runDbTest {
    insertItems(ITEMS_LIST)
    // load once to register db observers
    pagingSource.refresh()
    assertFalse(pagingSource.invalid)

    val result = deleteItem(TestItem(1000))

    // invalid delete. Should have 0 items deleted and paging source remains valid
    assertEquals(0, result)
    assertFalse(pagingSource.invalid)
  }

  @Test
  fun load_initialLoad() = runDbTest {
    insertItems(ITEMS_LIST)
    val result = pagingSource.refresh() as PagingSourceLoadResultPage<Int, TestItem>

    assertContentEquals(ITEMS_LIST.subList(0, 15), result.data)
  }

  @Test
  fun load_initialEmptyLoad() = runDbTest {
    val result = pagingSource.refresh() as PagingSourceLoadResultPage<Int, TestItem>

    assertTrue(result.data.isEmpty())

    // now add items
    insertItems(ITEMS_LIST)

    // invalidate pagingSource to imitate invalidation from running refreshVersionSync
    pagingSource.invalidate()
    assertTrue(pagingSource.invalid)

    // this refresh should check pagingSource's invalid status, realize it is invalid, and
    // return a PagingSourceLoadResultInvalid
    assertIs<PagingSourceLoadResultInvalid<Int, TestItem>>(pagingSource.refresh())
  }

  @Test
  fun load_initialLoadWithInitialKey() = runDbTest {
    insertItems(ITEMS_LIST)
    // refresh with initial key = 20
    val result = pagingSource.refresh(key = 20) as PagingSourceLoadResultPage<Int, TestItem>

    // item in pos 21-35 (TestItemId 20-34) loaded
    assertContentEquals(ITEMS_LIST.subList(20, 35), result.data)
  }

  @Test
  fun invalidInitialKey_dbEmpty_returnsEmpty() = runDbTest {
    val result = pagingSource.refresh(key = 101) as PagingSourceLoadResultPage<Int, TestItem>

    assertTrue(result.data.isEmpty())
  }

  @Test
  fun invalidInitialKey_keyTooLarge_returnsLastPage() = runDbTest {
    insertItems(ITEMS_LIST)
    val result = pagingSource.refresh(key = 101) as PagingSourceLoadResultPage<Int, TestItem>

    // should load the last page
    assertContentEquals(ITEMS_LIST.subList(85, 100), result.data)
  }

  @Test
  fun invalidInitialKey_negativeKey() = runDbTest {
    insertItems(ITEMS_LIST)
    // should throw error when initial key is negative
    val expectedException = assertFailsWith<IllegalArgumentException> {
      pagingSource.refresh(key = -1)
    }
    // default message from Paging 3 for negative initial key
    assertEquals("itemsBefore cannot be negative", expectedException.message)
  }

  @Test
  fun append_middleOfList() = runDbTest {
    insertItems(ITEMS_LIST)
    val result = pagingSource.append(key = 20) as PagingSourceLoadResultPage<Int, TestItem>

    // item in pos 21-25 (TestItemId 20-24) loaded
    assertContentEquals(ITEMS_LIST.subList(20, 25), result.data)
    assertEquals(25, result.nextKey)
    assertEquals(20, result.prevKey)
  }

  @Test
  fun append_availableItemsLessThanLoadSize() = runDbTest {
    insertItems(ITEMS_LIST)
    val result = pagingSource.append(key = 97) as PagingSourceLoadResultPage<Int, TestItem>

    // item in pos 98-100 (TestItemId 97-99) loaded
    assertContentEquals(ITEMS_LIST.subList(97, 100), result.data)
    assertNull(result.nextKey)
    assertEquals(97, result.prevKey)
  }

  @Test
  fun load_consecutiveAppend() = runDbTest {
    insertItems(ITEMS_LIST)
    // first append
    val result = pagingSource.append(key = 30) as PagingSourceLoadResultPage<Int, TestItem>

    // TestItemId 30-34 loaded
    assertContentEquals(ITEMS_LIST.subList(30, 35), result.data)
    // second append using nextKey from previous load
    val result2 = pagingSource.append(key = result.nextKey) as PagingSourceLoadResultPage<Int, TestItem>

    // TestItemId 35 - 39 loaded
    assertContentEquals(ITEMS_LIST.subList(35, 40), result2.data)
  }

  @Test
  fun append_invalidResult() = runDbTest {
    insertItems(ITEMS_LIST)
    // first append
    val result = pagingSource.append(key = 30) as PagingSourceLoadResultPage<Int, TestItem>

    // TestItemId 30-34 loaded
    assertContentEquals(ITEMS_LIST.subList(30, 35), result.data)

    // invalidate pagingSource to imitate invalidation from running refreshVersionSync
    pagingSource.invalidate()

    // this append should check pagingSource's invalid status, realize it is invalid, and
    // return a PagingSourceLoadResultInvalid
    val result2 = pagingSource.append(key = result.nextKey)

    assertIs<PagingSourceLoadResultInvalid<Int, TestItem>>(result2)
  }

  @Test
  fun prepend_middleOfList() = runDbTest {
    insertItems(ITEMS_LIST)
    val result = pagingSource.prepend(key = 30) as PagingSourceLoadResultPage<Int, TestItem>

    assertContentEquals(ITEMS_LIST.subList(25, 30), result.data)
    assertEquals(30, result.nextKey)
    assertEquals(25, result.prevKey)
  }

  @Test
  fun prepend_availableItemsLessThanLoadSize() = runDbTest {
    insertItems(ITEMS_LIST)
    val result = pagingSource.prepend(key = 3) as PagingSourceLoadResultPage<Int, TestItem>

    // items in pos 0 - 2 (TestItemId 0 - 2) loaded
    assertContentEquals(ITEMS_LIST.subList(0, 3), result.data)
    assertEquals(3, result.nextKey)
    assertNull(result.prevKey)
  }

  @Test
  fun load_consecutivePrepend() = runDbTest {
    insertItems(ITEMS_LIST)
    // first prepend
    val result = pagingSource.prepend(key = 20) as PagingSourceLoadResultPage<Int, TestItem>

    // items pos 16-20 (TestItemId 15-19) loaded
    assertContentEquals(ITEMS_LIST.subList(15, 20), result.data)
    // second prepend using prevKey from previous load
    val result2 = pagingSource.prepend(key = result.prevKey) as PagingSourceLoadResultPage<Int, TestItem>

    // items pos 11-15 (TestItemId 10 - 14) loaded
    assertContentEquals(ITEMS_LIST.subList(10, 15), result2.data)
  }

  @Test
  fun prepend_invalidResult() = runDbTest {
    insertItems(ITEMS_LIST)
    // first prepend
    val result = pagingSource.prepend(key = 20) as PagingSourceLoadResultPage<Int, TestItem>

    // items pos 16-20 (TestItemId 15-19) loaded
    assertContentEquals(ITEMS_LIST.subList(15, 20), result.data)

    // invalidate pagingSource to imitate invalidation from running refreshVersionSync
    pagingSource.invalidate()

    // this prepend should check pagingSource's invalid status, realize it is invalid, and
    // return PagingSourceLoadResultInvalid
    val result2 = pagingSource.prepend(key = result.prevKey)

    assertIs<PagingSourceLoadResultInvalid<Int, TestItem>>(result2)
  }

  @Test
  fun test_itemsBefore() = runDbTest {
    insertItems(ITEMS_LIST)
    // for initial load
    val result = pagingSource.refresh(key = 50) as PagingSourceLoadResultPage<Int, TestItem>

    // initial loads items in pos 51 - 65, should have 50 items before
    assertEquals(50, result.itemsBefore)

    // prepend from initial load
    val result2 = pagingSource.prepend(key = result.prevKey) as PagingSourceLoadResultPage<Int, TestItem>

    // prepend loads items in pos 46 - 50, should have 45 item before
    assertEquals(45, result2.itemsBefore)

    // append from initial load
    val result3 = pagingSource.append(key = result.nextKey) as PagingSourceLoadResultPage<Int, TestItem>

    // append loads items in position 66 - 70 , should have 65 item before
    assertEquals(65, result3.itemsBefore)
  }

  @Test
  fun test_itemsAfter() = runDbTest {
    insertItems(ITEMS_LIST)
    // for initial load
    val result = pagingSource.refresh(key = 30) as PagingSourceLoadResultPage<Int, TestItem>

    // initial loads items in position 31 - 45, should have 55 items after
    assertEquals(55, result.itemsAfter)

    // prepend from initial load
    val result2 = pagingSource.prepend(key = result.prevKey) as PagingSourceLoadResultPage<Int, TestItem>

    // prepend loads items in position 26 - 30, should have 70 item after
    assertEquals(70, result2.itemsAfter)

    // append from initial load
    val result3 = pagingSource.append(result.nextKey) as PagingSourceLoadResultPage<Int, TestItem>

    // append loads items in position 46 - 50 , should have 50 item after
    assertEquals(50, result3.itemsAfter)
  }

  @Test
  fun test_getRefreshKey() = runDbTest {
    insertItems(ITEMS_LIST)
    // initial load
    val result = pagingSource.refresh() as PagingSourceLoadResultPage<Int, TestItem>
    // 15 items loaded, assuming anchorPosition = 14 as the last item loaded
    var refreshKey = pagingSource.getRefreshKey(
      PagingState<Int, TestItem>(
        pages = listOf(result),
        anchorPosition = 14,
        config = CONFIG,
        leadingPlaceholderCount = 0,
      ),
    )
    // should load around anchor position
    // Initial load size = 15, refresh key should be (15/2 = 7) items
    // before anchorPosition (14 - 7 = 7)
    assertEquals(7, refreshKey)

    // append after refresh
    val result2 = pagingSource.append(key = result.nextKey) as PagingSourceLoadResultPage<Int, TestItem>

    assertContentEquals(ITEMS_LIST.subList(15, 20), result2.data)
    refreshKey = pagingSource.getRefreshKey(
      PagingState<Int, TestItem>(
        pages = listOf(result, result2),
        // 20 items loaded, assume anchorPosition = 19 as the last item loaded
        anchorPosition = 19,
        config = CONFIG,
        leadingPlaceholderCount = 0,
      ),
    )
    // initial load size 15. Refresh key should be (15/2 = 7) items before anchorPosition
    // (19 - 7 = 12)
    assertEquals(12, refreshKey)
  }

  @Test
  fun load_refreshKeyGreaterThanItemCount_lastPage() = runDbTest {
    insertItems(ITEMS_LIST)
    pagingSource.refresh(key = 70)

    deleteItems(40..100)

    // assume user was viewing last item of the refresh load with anchorPosition = 85,
    // initialLoadSize = 15. This mimics how getRefreshKey() calculates refresh key.
    val refreshKey = 85 - (15 / 2)
    assertEquals(78, refreshKey)

    val pagingSource2 = QueryPagingSource(
      countQuery(),
      transacter,
      EmptyCoroutineContext,
      ::query,
    )
    val result2 = pagingSource2.refresh(key = refreshKey) as PagingSourceLoadResultPage<Int, TestItem>

    // database should only have 40 items left. Refresh key is invalid at this point
    // (greater than item count after deletion)
    Pager(CONFIG, pagingSourceFactory = { pagingSource2 })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertEquals(40, itemCount)
      }
    // ensure that paging source can handle invalid refresh key properly
    // should load last page with items 25 - 40
    assertContentEquals(ITEMS_LIST.subList(25, 40), result2.data)

    // should account for updated item count to return correct itemsBefore, itemsAfter,
    // prevKey, nextKey
    assertEquals(25, result2.itemsBefore)
    assertEquals(0, result2.itemsAfter)
    // no append can be triggered
    assertEquals(25, result2.prevKey)
    assertNull(result2.nextKey)
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
  fun load_refreshKeyGreaterThanItemCount_firstPage() = runDbTest {
    insertItems(ITEMS_LIST)
    pagingSource.refresh()

    Pager(CONFIG, pagingSourceFactory = { pagingSource })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertEquals(100, itemCount)
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

    val result2 = pagingSource2.refresh(key = refreshKey) as PagingSourceLoadResultPage<Int, TestItem>

    // database should only have 70 items left
    Pager(CONFIG, pagingSourceFactory = { pagingSource2 })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertEquals(70, itemCount)
      }
    // first 30 items deleted, refresh should load starting from pos 31 (item id 30 - 45)
    assertContentEquals(ITEMS_LIST.subList(30, 45), result2.data)

    // should account for updated item count to return correct itemsBefore, itemsAfter,
    // prevKey, nextKey
    assertEquals(0, result2.itemsBefore)
    assertEquals(55, result2.itemsAfter)
    // no prepend can be triggered
    assertNull(result2.prevKey)
    assertEquals(15, result2.nextKey)
  }

  @Test
  fun load_loadSizeAndRefreshKeyGreaterThanItemCount() = runDbTest {
    insertItems(ITEMS_LIST)
    pagingSource.refresh(key = 30)

    Pager(CONFIG, pagingSourceFactory = { pagingSource })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertEquals(100, itemCount)
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

    val result2 = pagingSource2.refresh(key = refreshKey) as PagingSourceLoadResultPage<Int, TestItem>

    // database should only have 5 items left
    Pager(CONFIG, pagingSourceFactory = { pagingSource2 })
      .flow
      .first()
      .withPagingDataDiffer(this, testItemDiffCallback) {
        assertEquals(5, itemCount)
      }
    // only 5 items should be loaded with offset = 0
    assertContentEquals(ITEMS_LIST.subList(95, 100), result2.data)

    // should recognize that this is a terminal load
    assertEquals(0, result2.itemsBefore)
    assertEquals(0, result2.itemsAfter)
    assertNull(result2.prevKey)
    assertNull(result2.nextKey)
  }

  @Test
  fun test_jumpSupport() = runDbTest {
    assertTrue(pagingSource.jumpingSupported)
  }

  @Test
  fun load_initialEmptyLoad_QueryPagingSourceLong() = runDbTest {
    val result = pagingSource.refresh() as PagingSourceLoadResultPage<Int, TestItem>

    assertTrue(result.data.isEmpty())

    // now add items
    insertItems(ITEMS_LIST)

    // invalidate pagingSource to imitate invalidation from running refreshVersionSync
    pagingSource.invalidate()
    assertTrue(pagingSource.invalid)

    // this refresh should check pagingSource's invalid status, realize it is invalid, and
    // return a PagingSourceLoadResultInvalid
    assertIs<PagingSourceLoadResultInvalid<Int, TestItem>>(pagingSource.refresh())
  }

  private fun query(limit: Int, offset: Int) = queryLong(limit.toLong(), offset.toLong())

  private fun queryLong(limit: Long, offset: Long) = object : Query<TestItem>(
    { cursor ->
      TestItem(cursor.getLong(0)!!)
    },
  ) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>) = driver.executeQuery(20, "SELECT id FROM TestItem LIMIT ? OFFSET ?", mapper, 2) {
      bindLong(0, limit)
      bindLong(1, offset)
    }

    override fun addListener(listener: Listener) = driver.addListener("TestItem", listener = listener)
    override fun removeListener(listener: Listener) = driver.removeListener("TestItem", listener = listener)
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
      driver.execute(21, "INSERT INTO TestItem (id) VALUES (?)", 1) {
        bindLong(0, it.id)
      }
    }
  }

  private fun deleteItem(item: TestItem): Long =
    driver
      .execute(22, "DELETE FROM TestItem WHERE id = ?;", 1) {
        bindLong(0, item.id)
      }
      .value

  private fun deleteItems(range: IntRange): Long =
    driver
      .execute(23, "DELETE FROM TestItem WHERE id >= ? AND id <= ?", 2) {
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

private fun createLoadParam(loadType: LoadType, key: Int?): PagingSourceLoadParams<Int> = when (loadType) {
  LoadType.REFRESH -> PagingSourceLoadParamsRefresh(
    key = key,
    loadSize = CONFIG.initialLoadSize,
    placeholdersEnabled = CONFIG.enablePlaceholders,
  ) as PagingSourceLoadParams<Int>

  LoadType.APPEND -> PagingSourceLoadParamsAppend(
    key = key ?: -1,
    loadSize = CONFIG.pageSize,
    placeholdersEnabled = CONFIG.enablePlaceholders,
  ) as PagingSourceLoadParams<Int>

  LoadType.PREPEND -> PagingSourceLoadParamsPrepend(
    key = key ?: -1,
    loadSize = CONFIG.pageSize,
    placeholdersEnabled = CONFIG.enablePlaceholders,
  ) as PagingSourceLoadParams<Int>

  else -> error("Unknown PagingSourceLoadParams ${loadType::class}")
}

private suspend fun PagingSource<Int, TestItem>.refresh(key: Int? = null): PagingSourceLoadResult<Int, TestItem> =
  load(createLoadParam(LoadType.REFRESH, key))

private suspend fun PagingSource<Int, TestItem>.append(key: Int?): PagingSourceLoadResult<Int, TestItem> =
  load(createLoadParam(LoadType.APPEND, key))

private suspend fun PagingSource<Int, TestItem>.prepend(key: Int?): PagingSourceLoadResult<Int, TestItem> =
  load(createLoadParam(LoadType.PREPEND, key))
