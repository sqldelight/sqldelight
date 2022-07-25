// Copyright Square, Inc.
package app.cash.sqldelight.paging3

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.paging3.util.INITIAL_ITEM_COUNT
import app.cash.sqldelight.paging3.util.INVALID
import app.cash.sqldelight.paging3.util.getClippedRefreshKey
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.paging3.util.getLimit
import app.cash.sqldelight.paging3.util.getOffset
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * An implementation of [PagingSource] to perform a LIMIT OFFSET query
 *
 * This class is used for Paging3 to perform Query and RawQuery in Room to return a PagingSource
 * for Pager's consumption.
 */
internal class LimitOffsetPagingSource<RowType : Any>(
  private val queryProvider: (limit: Int, offset: Int) -> Query<RowType>,
  private val countQuery: Query<Int>,
  private val transacter: Transacter,
  private val context: CoroutineContext,
) : QueryPagingSource<Int, RowType>() {

  internal val itemCount: AtomicInteger = AtomicInteger(INITIAL_ITEM_COUNT)

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RowType> {
    return withContext(context) {
      val tempCount = itemCount.get()
      // if itemCount is < 0, then it is initial load
      if (tempCount == INITIAL_ITEM_COUNT) {
        initialLoad(params)
      } else {
        nonInitialLoad(params, tempCount)
      }
    }
  }

  /**
   *  For the very first time that this PagingSource's [load] is called. Executes the count
   *  query (initializes [itemCount]) and db query within a transaction to ensure initial load's
   *  data integrity.
   *
   *  For example, if the database gets updated after the count query but before the db query
   *  completes, the paging source may not invalidate in time, but this method will return
   *  data based on the original database that the count was performed on to ensure a valid
   *  initial load.
   */
  private suspend fun initialLoad(params: LoadParams<Int>): LoadResult<Int, RowType> {
    return transacter.transactionWithResult {
      val tempCount = countQuery.executeAsOne()
      itemCount.set(tempCount)
      queryDatabase(
        params = params,
        itemCount = tempCount,
      )
    }
  }

  private suspend fun nonInitialLoad(
    params: LoadParams<Int>,
    tempCount: Int,
  ): LoadResult<Int, RowType> {
    val loadResult = queryDatabase(
      params = params,
      itemCount = tempCount,
    )
    @Suppress("UNCHECKED_CAST")
    return if (invalid) INVALID as LoadResult.Invalid<Int, RowType> else loadResult
  }

  override fun getRefreshKey(state: PagingState<Int, RowType>): Int? {
    return state.getClippedRefreshKey()
  }

  override val jumpingSupported: Boolean
    get() = true

  /**
   * calls RoomDatabase.query() to return a cursor and then calls convertRows() to extract and
   * return list of data
   *
   * throws [IllegalArgumentException] from CursorUtil if column does not exist
   *
   * @param params load params to calculate query limit and offset
   *
   * @param itemCount the db row count, triggers a new PagingSource generation if itemCount changes,
   * i.e. items are added / removed
   */
  private fun queryDatabase(
    params: LoadParams<Int>,
    itemCount: Int,
  ): LoadResult<Int, RowType> {
    val key = params.key ?: 0
    val limit: Int = getLimit(params, key)
    val offset: Int = getOffset(params, key, itemCount)
    val data = queryProvider(limit, offset)
      .also { currentQuery = it }
      .executeAsList()
    val nextPosToLoad = offset + data.size
    val nextKey =
      if (data.isEmpty() || data.size < limit || nextPosToLoad >= itemCount) {
        null
      } else {
        nextPosToLoad
      }
    val prevKey = if (offset <= 0 || data.isEmpty()) null else offset
    return LoadResult.Page(
      data = data,
      prevKey = prevKey,
      nextKey = nextKey,
      itemsBefore = offset,
      itemsAfter = maxOf(0, itemCount - nextPosToLoad)
    )
  }
}
