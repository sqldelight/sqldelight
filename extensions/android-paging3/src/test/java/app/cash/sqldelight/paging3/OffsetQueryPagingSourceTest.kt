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

import android.database.Cursor
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.awaitPendingRefresh
import androidx.room.util.getColumnIndexOrThrow
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.FilteringExecutor
import androidx.testutils.TestExecutor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Job

private const val tableName: String = "TestItem"

@RunWith(AndroidJUnit4::class)
@SmallTest
class LimitOffsetPagingSourceTest {

  @JvmField
  @Rule
  val countingTaskExecutorRule = CountingTaskExecutorRule()

  private lateinit var database: LimitOffsetTestDb
  private lateinit var dao: TestItemDao

  @Before
  fun init() {
    database = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      LimitOffsetTestDb::class.java,
    ).build()
    dao = database.dao
  }

  @After
  fun tearDown() {
    database.close()
    // At the end of all tests, query executor should be idle (transaction thread released).
    countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
    assertThat(countingTaskExecutorRule.isIdle).isTrue()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun load_usesQueryExecutor() = runTest {
    val queryExecutor = TestExecutor()
    val transactionExecutor = TestExecutor()
    database = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      LimitOffsetTestDb::class.java,
    ).setQueryExecutor(queryExecutor)
      .setTransactionExecutor(transactionExecutor)
      .build()

    // Ensure there are no init tasks enqueued on queryExecutor before we call .load().
    assertThat(queryExecutor.executeAll()).isFalse()
    assertThat(transactionExecutor.executeAll()).isFalse()

    val job = Job()
    launch(job) {
      LimitOffsetPagingSourceImpl(database).load(
        PagingSource.LoadParams.Refresh(
          key = null,
          loadSize = 1,
          placeholdersEnabled = true
        )
      )
    }

    // Let the launched job start and proceed as far as possible.
    advanceUntilIdle()

    // Check that .load() dispatches on queryExecutor before jumping into a transaction for
    // initial load.
    assertThat(transactionExecutor.executeAll()).isFalse()
    assertThat(queryExecutor.executeAll()).isTrue()

    job.cancel()
  }

  @Test
  fun test_itemCount() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    runBlocking {
      // count query is executed on first load
      pagingSource.refresh()

      assertThat(pagingSource.itemCount.get()).isEqualTo(100)
    }
  }

  @Test
  fun test_itemCountWithSuppliedLimitOffset() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(
      db = database,
      queryString = "SELECT * FROM $tableName ORDER BY id ASC LIMIT 60 OFFSET 30",
    )
    runBlocking {
      // count query is executed on first load
      pagingSource.refresh()
      // should be 60 instead of 100
      assertThat(pagingSource.itemCount.get()).isEqualTo(60)
    }
  }

  @Test
  fun dbInsert_pagingSourceInvalidates() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    runBlocking {
      // load once to register db observers
      pagingSource.refresh()
      assertThat(pagingSource.invalid).isFalse()
      // paging source should be invalidated when insert into db
      val result = dao.addTestItem(TestItem(101))
      countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
      assertThat(result).isEqualTo(101)
      assertTrue(pagingSource.invalid)
    }
  }

  @Test
  fun dbDelete_pagingSourceInvalidates() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    runBlocking {
      // load once to register db observers
      pagingSource.refresh()
      assertThat(pagingSource.invalid).isFalse()
      // paging source should be invalidated when delete from db
      dao.deleteTestItem(TestItem(50))
      countingTaskExecutorRule.drainTasks(5, TimeUnit.SECONDS)
      assertTrue(pagingSource.invalid)
    }
  }

  @Test
  fun invalidDbQuery_pagingSourceDoesNotInvalidate() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    runBlocking {
      // load once to register db observers
      pagingSource.refresh()
      assertThat(pagingSource.invalid).isFalse()

      val result = dao.deleteTestItem(TestItem(1000))

      // invalid delete. Should have 0 items deleted and paging source remains valid
      assertThat(result).isEqualTo(0)
      assertFalse(pagingSource.invalid)
    }
  }

  @Test
  fun load_initialLoad() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    runBlocking {
      val result = pagingSource.refresh() as LoadResult.Page

      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(0, 15)
      )
    }
  }

  @Test
  fun load_initialEmptyLoad() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    runBlocking {
      val result = pagingSource.refresh() as LoadResult.Page

      assertTrue(result.data.isEmpty())

      // now add items
      dao.addAllItems(ITEMS_LIST)

      // invalidate pagingSource to imitate invalidation from running refreshVersionSync
      pagingSource.invalidate()
      assertTrue(pagingSource.invalid)

      // this refresh should check pagingSource's invalid status, realize it is invalid, and
      // return a LoadResult.Invalid
      assertThat(pagingSource.refresh()).isInstanceOf(
        LoadResult.Invalid::class.java
      )
    }
  }

  @Test
  fun load_initialLoadWithInitialKey() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    // refresh with initial key = 20
    runBlocking {
      val result = pagingSource.refresh(key = 20) as LoadResult.Page

      // item in pos 21-35 (TestItemId 20-34) loaded
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(20, 35)
      )
    }
  }

  @Test
  fun load_initialLoadWithSuppliedLimitOffset() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(
      db = database,
      queryString = "SELECT * FROM $tableName ORDER BY id ASC LIMIT 10 OFFSET 30",
    )
    runBlocking {
      val result = pagingSource.refresh() as LoadResult.Page

      // default initial loadSize = 15 starting from index 0.
      // user supplied limit offset should cause initial loadSize = 10, starting from index 30
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(30, 40)
      )
      // check that no append/prepend can be triggered after this terminal load
      assertThat(result.nextKey).isNull()
      assertThat(result.prevKey).isNull()
      assertThat(result.itemsBefore).isEqualTo(0)
      assertThat(result.itemsAfter).isEqualTo(0)
    }
  }

  @Test
  fun load_oneAdditionalQueryArguments() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(
      db = database,
      queryString = "SELECT * FROM $tableName WHERE id < 50 ORDER BY id ASC",
    )
    // refresh with initial key = 40
    runBlocking {
      val result = pagingSource.refresh(key = 40) as LoadResult.Page

      // initial loadSize = 15, but limited by id < 50, should only load items 40 - 50
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(40, 50)
      )
      // should have 50 items fulfilling condition of id < 50 (TestItem id 0 - 49)
      assertThat(pagingSource.itemCount.get()).isEqualTo(50)
    }
  }

  @Test
  fun load_multipleQueryArguments() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(
      db = database,
      queryString = "SELECT * " +
          "FROM $tableName " +
          "WHERE id > 50 AND value LIKE 'item 90'" +
          "ORDER BY id ASC",
    )
    runBlocking {
      val result = pagingSource.refresh() as LoadResult.Page

      assertThat(result.data).containsExactly(ITEMS_LIST[90])
      assertThat(pagingSource.itemCount.get()).isEqualTo(1)
    }
  }

  @Test
  fun load_InvalidUserSuppliedOffset_returnEmpty() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(
      db = database,
      queryString = "SELECT * FROM $tableName ORDER BY id ASC LIMIT 10 OFFSET 500",
    )
    runBlocking {
      val result = pagingSource.refresh() as LoadResult.Page

      // invalid OFFSET = 500 should return empty data
      assertThat(result.data).isEmpty()

      // check that no append/prepend can be triggered
      assertThat(pagingSource.itemCount.get()).isEqualTo(0)
      assertThat(result.nextKey).isNull()
      assertThat(result.prevKey).isNull()
      assertThat(result.itemsBefore).isEqualTo(0)
      assertThat(result.itemsAfter).isEqualTo(0)
    }
  }

  @Test
  fun load_UserSuppliedNegativeLimit() {
    dao.addAllItems(ITEMS_LIST)
    val pagingSource = LimitOffsetPagingSourceImpl(
      db = database,
      queryString = "SELECT * FROM $tableName ORDER BY id ASC LIMIT -1",
    )
    runBlocking {
      val result = pagingSource.refresh() as LoadResult.Page

      // ensure that it respects SQLite's default behavior for negative LIMIT
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(0, 15)
      )
      // should behave as if no LIMIT were set
      assertThat(pagingSource.itemCount.get()).isEqualTo(100)
      assertThat(result.nextKey).isEqualTo(15)
      assertThat(result.prevKey).isNull()
      assertThat(result.itemsBefore).isEqualTo(0)
      assertThat(result.itemsAfter).isEqualTo(85)
    }
  }

  @Test
  fun invalidInitialKey_dbEmpty_returnsEmpty() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    runBlocking {
      val result = pagingSource.refresh(key = 101) as LoadResult.Page

      assertThat(result.data).isEmpty()
    }
  }

  @Test
  fun invalidInitialKey_keyTooLarge_returnsLastPage() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    runBlocking {
      val result = pagingSource.refresh(key = 101) as LoadResult.Page

      // should load the last page
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(85, 100)
      )
    }
  }

  @Test
  fun invalidInitialKey_negativeKey() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    runBlocking {
      // should throw error when initial key is negative
      val expectedException = assertFailsWith<IllegalArgumentException> {
        pagingSource.refresh(key = -1)
      }
      // default message from Paging 3 for negative initial key
      assertThat(expectedException.message).isEqualTo(
        "itemsBefore cannot be negative"
      )
    }
  }

  @Test
  fun append_middleOfList() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    // to bypass check for initial load and run as non-initial load
    pagingSource.itemCount.set(100)
    runBlocking {
      val result = pagingSource.append(key = 20) as LoadResult.Page

      // item in pos 21-25 (TestItemId 20-24) loaded
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(20, 25)
      )
      assertThat(result.nextKey).isEqualTo(25)
      assertThat(result.prevKey).isEqualTo(20)
    }
  }

  @Test
  fun append_availableItemsLessThanLoadSize() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    // to bypass check for initial load and run as non-initial load
    pagingSource.itemCount.set(100)
    runBlocking {
      val result = pagingSource.append(key = 97) as LoadResult.Page

      // item in pos 98-100 (TestItemId 97-99) loaded
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(97, 100)
      )
      assertThat(result.nextKey).isEqualTo(null)
      assertThat(result.prevKey).isEqualTo(97)
    }
  }

  @Test
  fun load_consecutiveAppend() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    // to bypass check for initial load and run as non-initial load
    pagingSource.itemCount.set(100)
    runBlocking {
      // first append
      val result = pagingSource.append(key = 30) as LoadResult.Page

      // TestItemId 30-34 loaded
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(30, 35)
      )
      // second append using nextKey from previous load
      val result2 = pagingSource.append(key = result.nextKey) as LoadResult.Page

      // TestItemId 35 - 39 loaded
      assertThat(result2.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(35, 40)
      )
    }
  }

  @Test
  fun append_invalidResult() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    // to bypass check for initial load and run as non-initial load
    pagingSource.itemCount.set(100)
    runBlocking {
      // first append
      val result = pagingSource.append(key = 30) as LoadResult.Page

      // TestItemId 30-34 loaded
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(30, 35)
      )

      // invalidate pagingSource to imitate invalidation from running refreshVersionSync
      pagingSource.invalidate()

      // this append should check pagingSource's invalid status, realize it is invalid, and
      // return a LoadResult.Invalid
      val result2 = pagingSource.append(key = result.nextKey)

      assertThat(result2).isInstanceOf(LoadResult.Invalid::class.java)
    }
  }

  @Test
  fun prepend_middleOfList() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    // to bypass check for initial load and run as non-initial load
    pagingSource.itemCount.set(100)
    runBlocking {
      val result = pagingSource.prepend(key = 30) as LoadResult.Page

      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(25, 30)
      )
      assertThat(result.nextKey).isEqualTo(30)
      assertThat(result.prevKey).isEqualTo(25)
    }
  }

  @Test
  fun prepend_availableItemsLessThanLoadSize() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    // to bypass check for initial load and run as non-initial load
    pagingSource.itemCount.set(100)
    runBlocking {
      val result = pagingSource.prepend(key = 3) as LoadResult.Page

      // items in pos 0 - 2 (TestItemId 0 - 2) loaded
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(0, 3)
      )
      assertThat(result.nextKey).isEqualTo(3)
      assertThat(result.prevKey).isEqualTo(null)
    }
  }

  @Test
  fun load_consecutivePrepend() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    // to bypass check for initial load and run as non-initial load
    pagingSource.itemCount.set(100)
    runBlocking {
      // first prepend
      val result = pagingSource.prepend(key = 20) as LoadResult.Page

      // items pos 16-20 (TestItemId 15-19) loaded
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(15, 20)
      )
      // second prepend using prevKey from previous load
      val result2 = pagingSource.prepend(key = result.prevKey) as LoadResult.Page

      // items pos 11-15 (TestItemId 10 - 14) loaded
      assertThat(result2.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(10, 15)
      )
    }
  }

  @Test
  fun prepend_invalidResult() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    // to bypass check for initial load and run as non-initial load
    pagingSource.itemCount.set(100)
    runBlocking {
      // first prepend
      val result = pagingSource.prepend(key = 20) as LoadResult.Page

      // items pos 16-20 (TestItemId 15-19) loaded
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(15, 20)
      )

      // invalidate pagingSource to imitate invalidation from running refreshVersionSync
      pagingSource.invalidate()

      // this prepend should check pagingSource's invalid status, realize it is invalid, and
      // return LoadResult.Invalid
      val result2 = pagingSource.prepend(key = result.prevKey)

      assertThat(result2).isInstanceOf(LoadResult.Invalid::class.java)
    }
  }

  @Test
  fun test_itemsBefore() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    runBlocking {
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
  }

  @Test
  fun test_itemsAfter() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    runBlocking {
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
  }

  @Test
  fun test_getRefreshKey() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    runBlocking {
      // initial load
      val result = pagingSource.refresh() as LoadResult.Page
      // 15 items loaded, assuming anchorPosition = 14 as the last item loaded
      var refreshKey = pagingSource.getRefreshKey(
        PagingState(
          pages = listOf(result),
          anchorPosition = 14,
          config = CONFIG,
          leadingPlaceholderCount = 0
        )
      )
      // should load around anchor position
      // Initial load size = 15, refresh key should be (15/2 = 7) items
      // before anchorPosition (14 - 7 = 7)
      assertThat(refreshKey).isEqualTo(7)

      // append after refresh
      val result2 = pagingSource.append(key = result.nextKey) as LoadResult.Page

      assertThat(result2.data).isEqualTo(
        ITEMS_LIST.subList(15, 20)
      )
      refreshKey = pagingSource.getRefreshKey(
        PagingState(
          pages = listOf(result, result2),
          // 20 items loaded, assume anchorPosition = 19 as the last item loaded
          anchorPosition = 19,
          config = CONFIG,
          leadingPlaceholderCount = 0
        )
      )
      // initial load size 15. Refresh key should be (15/2 = 7) items before anchorPosition
      // (19 - 7 = 12)
      assertThat(refreshKey).isEqualTo(12)
    }
  }

  @Test
  fun load_refreshKeyGreaterThanItemCount_lastPage() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    runBlocking {

      pagingSource.refresh(key = 70)

      dao.deleteTestItems(40, 100)

      // assume user was viewing last item of the refresh load with anchorPosition = 85,
      // initialLoadSize = 15. This mimics how getRefreshKey() calculates refresh key.
      val refreshKey = 85 - (15 / 2)
      assertThat(refreshKey).isEqualTo(78)

      val pagingSource2 = LimitOffsetPagingSourceImpl(database)
      val result2 = pagingSource2.refresh(key = refreshKey) as LoadResult.Page

      // database should only have 40 items left. Refresh key is invalid at this point
      // (greater than item count after deletion)
      assertThat(pagingSource2.itemCount.get()).isEqualTo(40)
      // ensure that paging source can handle invalid refresh key properly
      // should load last page with items 25 - 40
      assertThat(result2.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(25, 40)
      )

      // should account for updated item count to return correct itemsBefore, itemsAfter,
      // prevKey, nextKey
      assertThat(result2.itemsBefore).isEqualTo(25)
      assertThat(result2.itemsAfter).isEqualTo(0)
      // no append can be triggered
      assertThat(result2.prevKey).isEqualTo(25)
      assertThat(result2.nextKey).isEqualTo(null)
    }
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
  fun load_refreshKeyGreaterThanItemCount_firstPage() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    runBlocking {
      pagingSource.refresh()

      assertThat(pagingSource.itemCount.get()).isEqualTo(100)

      // items id 0 - 29 deleted (30 items removed)
      dao.deleteTestItems(0, 29)

      val pagingSource2 = LimitOffsetPagingSourceImpl(database)
      // assume user was viewing first few items with anchorPosition = 0 and refresh key
      // clips to 0
      val refreshKey = 0

      val result2 = pagingSource2.refresh(key = refreshKey) as LoadResult.Page

      // database should only have 70 items left
      assertThat(pagingSource2.itemCount.get()).isEqualTo(70)
      // first 30 items deleted, refresh should load starting from pos 31 (item id 30 - 45)
      assertThat(result2.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(30, 45)
      )

      // should account for updated item count to return correct itemsBefore, itemsAfter,
      // prevKey, nextKey
      assertThat(result2.itemsBefore).isEqualTo(0)
      assertThat(result2.itemsAfter).isEqualTo(55)
      // no prepend can be triggered
      assertThat(result2.prevKey).isEqualTo(null)
      assertThat(result2.nextKey).isEqualTo(15)
    }
  }

  @Test
  fun load_loadSizeAndRefreshKeyGreaterThanItemCount() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    dao.addAllItems(ITEMS_LIST)
    runBlocking {

      pagingSource.refresh(key = 30)

      assertThat(pagingSource.itemCount.get()).isEqualTo(100)
      // items id 0 - 94 deleted (95 items removed)
      dao.deleteTestItems(0, 94)

      val pagingSource2 = LimitOffsetPagingSourceImpl(database)
      // assume user was viewing first few items with anchorPosition = 0 and refresh key
      // clips to 0
      val refreshKey = 0

      val result2 = pagingSource2.refresh(key = refreshKey) as LoadResult.Page

      // database should only have 5 items left
      assertThat(pagingSource2.itemCount.get()).isEqualTo(5)
      // only 5 items should be loaded with offset = 0
      assertThat(result2.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(95, 100)
      )

      // should recognize that this is a terminal load
      assertThat(result2.itemsBefore).isEqualTo(0)
      assertThat(result2.itemsAfter).isEqualTo(0)
      assertThat(result2.prevKey).isEqualTo(null)
      assertThat(result2.nextKey).isEqualTo(null)
    }
  }

  @Test
  fun test_jumpSupport() {
    val pagingSource = LimitOffsetPagingSourceImpl(database)
    assertTrue(pagingSource.jumpingSupported)
  }
}

@RunWith(AndroidJUnit4::class)
@SmallTest
class LimitOffsetPagingSourceTestWithFilteringExecutor {

  private lateinit var db: LimitOffsetTestDb
  private lateinit var dao: TestItemDao

  // Multiple threads are necessary to prevent deadlock, since Room will acquire a thread to
  // dispatch on, when using the query / transaction dispatchers.
  private val queryExecutor = FilteringExecutor(delegate = Executors.newFixedThreadPool(2))
  private val mainThreadQueries = mutableListOf<Pair<String, String>>()

  @Before
  fun init() {
    val mainThread: Thread = runBlocking(Dispatchers.Main) {
      Thread.currentThread()
    }
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      LimitOffsetTestDb::class.java
    ).setQueryCallback(
      object : RoomDatabase.QueryCallback {
        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
          if (Thread.currentThread() === mainThread) {
            mainThreadQueries.add(
              sqlQuery to Throwable().stackTraceToString()
            )
          }
        }
      }
    ) {
      // instantly execute the log callback so that we can check the thread.
      it.run()
    }.setQueryExecutor(queryExecutor)
      .build()
    dao = db.dao
  }

  @After
  fun tearDown() {
    // Check no mainThread queries happened.
    assertThat(mainThreadQueries).isEmpty()
    db.close()
  }

  @Test
  fun invalid_refresh() {
    val pagingSource = LimitOffsetPagingSourceImpl(db)
    runBlocking {
      val result = pagingSource.refresh() as LoadResult.Page

      assertTrue(result.data.isEmpty())

      // blocks invalidation notification from Room
      queryExecutor.filterFunction = { runnable ->
        runnable !== db.invalidationTracker.refreshRunnable
      }

      // now write to database
      dao.addAllItems(ITEMS_LIST)

      // make sure room requests a refresh
      db.invalidationTracker.awaitPendingRefresh()
      // and that this is blocked to simulate delayed notification from room
      queryExecutor.awaitDeferredSizeAtLeast(1)

      // the db write should cause pagingSource to realize it is invalid
      assertThat(pagingSource.refresh()).isInstanceOf(
        LoadResult.Invalid::class.java
      )
      assertTrue(pagingSource.invalid)
    }
  }

  @Test
  fun invalid_append() {
    val pagingSource = LimitOffsetPagingSourceImpl(db)
    dao.addAllItems(ITEMS_LIST)

    runBlocking {
      val result = pagingSource.refresh() as LoadResult.Page

      // initial load
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(0, 15)
      )

      // blocks invalidation notification from Room
      queryExecutor.filterFunction = { runnable ->
        runnable !== db.invalidationTracker.refreshRunnable
      }

      // now write to the database
      dao.deleteTestItem(ITEMS_LIST[30])

      // make sure room requests a refresh
      db.invalidationTracker.awaitPendingRefresh()
      // and that this is blocked to simulate delayed notification from room
      queryExecutor.awaitDeferredSizeAtLeast(1)

      // the db write should cause pagingSource to realize it is invalid when it tries to
      // append
      assertThat(pagingSource.append(15)).isInstanceOf(
        LoadResult.Invalid::class.java
      )
      assertTrue(pagingSource.invalid)
    }
  }

  @Test
  fun invalid_prepend() {
    val pagingSource = LimitOffsetPagingSourceImpl(db)
    dao.addAllItems(ITEMS_LIST)

    runBlocking {
      val result = pagingSource.refresh(key = 20) as LoadResult.Page

      // initial load
      assertThat(result.data).containsExactlyElementsIn(
        ITEMS_LIST.subList(20, 35)
      )

      // blocks invalidation notification from Room
      queryExecutor.filterFunction = { runnable ->
        runnable !== db.invalidationTracker.refreshRunnable
      }

      // now write to the database
      dao.deleteTestItem(ITEMS_LIST[30])

      // make sure room requests a refresh
      db.invalidationTracker.awaitPendingRefresh()
      // and that this is blocked to simulate delayed notification from room
      queryExecutor.awaitDeferredSizeAtLeast(1)

      // the db write should cause pagingSource to realize it is invalid when it tries to
      // append
      assertThat(pagingSource.prepend(20)).isInstanceOf(
        LoadResult.Invalid::class.java
      )
      assertTrue(pagingSource.invalid)
    }
  }
}

class LimitOffsetPagingSourceImpl(
  db: RoomDatabase,
  queryString: String = "SELECT * FROM $tableName ORDER BY id ASC",
) : LimitOffsetPagingSource<TestItem>(
  sourceQuery = RoomSQLiteQuery.acquire(
    queryString,
    0
  ),
  db = db,
  tables = arrayOf("$tableName")
) {

  override fun convertRows(cursor: Cursor): List<TestItem> {
    val cursorIndexOfId = getColumnIndexOrThrow(cursor, "id")
    val data = mutableListOf<TestItem>()
    while (cursor.moveToNext()) {
      val tmpId = cursor.getInt(cursorIndexOfId)
      data.add(TestItem(tmpId))
    }
    return data
  }
}

private val CONFIG = PagingConfig(
  pageSize = 5,
  enablePlaceholders = true,
  initialLoadSize = 15
)

private val ITEMS_LIST = createItemsForDb(0, 100)

private fun createLoadParam(
  loadType: LoadType,
  key: Int? = null,
  initialLoadSize: Int = CONFIG.initialLoadSize,
  pageSize: Int = CONFIG.pageSize,
  placeholdersEnabled: Boolean = CONFIG.enablePlaceholders
): PagingSource.LoadParams<Int> {
  return when (loadType) {
    LoadType.REFRESH -> {
      PagingSource.LoadParams.Refresh(
        key = key,
        loadSize = initialLoadSize,
        placeholdersEnabled = placeholdersEnabled
      )
    }
    LoadType.APPEND -> {
      PagingSource.LoadParams.Append(
        key = key ?: -1,
        loadSize = pageSize,
        placeholdersEnabled = placeholdersEnabled
      )
    }
    LoadType.PREPEND -> {
      PagingSource.LoadParams.Prepend(
        key = key ?: -1,
        loadSize = pageSize,
        placeholdersEnabled = placeholdersEnabled
      )
    }
  }
}

private fun createItemsForDb(startId: Int, count: Int): List<TestItem> {
  return List(count) {
    TestItem(
      id = it + startId,
    )
  }
}

private suspend fun PagingSource<Int, TestItem>.refresh(
  key: Int? = null,
): LoadResult<Int, TestItem> {
  return this.load(
    createLoadParam(
      loadType = LoadType.REFRESH,
      key = key,
    )
  )
}

private suspend fun PagingSource<Int, TestItem>.append(
  key: Int? = -1,
): LoadResult<Int, TestItem> {
  return this.load(
    createLoadParam(
      loadType = LoadType.APPEND,
      key = key,
    )
  )
}

private suspend fun PagingSource<Int, TestItem>.prepend(
  key: Int? = -1,
): LoadResult<Int, TestItem> {
  return this.load(
    createLoadParam(
      loadType = LoadType.PREPEND,
      key = key,
    )
  )
}
