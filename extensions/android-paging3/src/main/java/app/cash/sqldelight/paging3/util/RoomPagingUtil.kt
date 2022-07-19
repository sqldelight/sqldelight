// Copyright Square, Inc.
package app.cash.sqldelight.paging3.util

import android.database.Cursor
import android.os.CancellationSignal
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadParams.Prepend
import androidx.paging.PagingSource.LoadParams.Append
import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery

/**
 * A [LoadResult] that can be returned to trigger a new generation of PagingSource
 *
 * Any loaded data or queued loads prior to returning INVALID will be discarded
 */
val INVALID = LoadResult.Invalid<Any, Any>()

/**
 * The default itemCount value
 */
const val INITIAL_ITEM_COUNT = -1

/**
 * Calculates query limit based on LoadType.
 *
 * Prepend: If requested loadSize is larger than available number of items to prepend, it will
 * query with OFFSET = 0, LIMIT = prevKey
 */
fun getLimit(params: LoadParams<Int>, key: Int): Int {
  return when (params) {
    is Prepend ->
      if (key < params.loadSize) {
        key
      } else {
        params.loadSize
      }
    else -> params.loadSize
  }
}

/**
 * calculates query offset amount based on loadtype
 *
 * Prepend: OFFSET is calculated by counting backwards the number of items that needs to be
 * loaded before [key]. For example, if key = 30 and loadSize = 5, then offset = 25 and items
 * in db position 26-30 are loaded.
 * If requested loadSize is larger than the number of available items to
 * prepend, OFFSET clips to 0 to prevent negative OFFSET.
 *
 * Refresh:
 * If initialKey is supplied through Pager, Paging 3 will now start loading from
 * initialKey with initialKey being the first item.
 * If key is supplied by [getClippedRefreshKey], the key has already been adjusted to load half
 * of the requested items before anchorPosition and the other half after anchorPosition. See
 * comments on [getClippedRefreshKey] for more details.
 * If key (regardless if from initialKey or [getClippedRefreshKey]) is larger than available items,
 * the last page will be loaded by counting backwards the loadSize before last item in
 * database. For example, this can happen if invalidation came from a large number of items
 * dropped. i.e. in items 0 - 100, items 41-80 are dropped. Depending on last
 * viewed item, hypothetically [getClippedRefreshKey] may return key = 60. If loadSize = 10, then items
 * 31-40 will be loaded.
 */
fun getOffset(params: LoadParams<Int>, key: Int, itemCount: Int): Int {
  return when (params) {
    is Prepend ->
      if (key < params.loadSize) {
        0
      } else {
        key - params.loadSize
      }
    is Append -> key
    is Refresh ->
      if (key >= itemCount) {
        maxOf(0, itemCount - params.loadSize)
      } else {
        key
      }
  }
}

/**
 * calls RoomDatabase.query() to return a cursor and then calls convertRows() to extract and
 * return list of data
 *
 * throws [IllegalArgumentException] from CursorUtil if column does not exist
 *
 * @param params load params to calculate query limit and offset
 *
 * @param sourceQuery user provided [RoomSQLiteQuery] for database query
 *
 * @param db the [RoomDatabase] to query from
 *
 * @param itemCount the db row count, triggers a new PagingSource generation if itemCount changes,
 * i.e. items are added / removed
 *
 * @param cancellationSignal the signal to cancel the query if the query hasn't yet completed
 *
 * @param convertRows the function to iterate data with provided [Cursor] to return List<Value>
 */
fun <Value : Any> queryDatabase(
  params: LoadParams<Int>,
  sourceQuery: RoomSQLiteQuery,
  db: RoomDatabase,
  itemCount: Int,
  cancellationSignal: CancellationSignal? = null,
  convertRows: (Cursor) -> List<Value>,
): LoadResult<Int, Value> {
  val key = params.key ?: 0
  val limit: Int = getLimit(params, key)
  val offset: Int = getOffset(params, key, itemCount)
  val limitOffsetQuery =
    "SELECT * FROM ( ${sourceQuery.sql} ) LIMIT $limit OFFSET $offset"
  val sqLiteQuery: RoomSQLiteQuery = RoomSQLiteQuery.acquire(
    limitOffsetQuery,
    sourceQuery.argCount
  )
  sqLiteQuery.copyArgumentsFrom(sourceQuery)
  val cursor = db.query(sqLiteQuery, cancellationSignal)
  val data: List<Value>
  try {
    data = convertRows(cursor)
  } finally {
    cursor.close()
    sqLiteQuery.release()
  }
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

/**
 * returns count of requested items to calculate itemsAfter and itemsBefore for use in creating
 * LoadResult.Page<>
 *
 * throws error when the column value is null, the column type is not an integral type,
 * or the integer value is outside the range [Integer.MIN_VALUE, Integer.MAX_VALUE]
 */
fun queryItemCount(
  sourceQuery: RoomSQLiteQuery,
  db: RoomDatabase
): Int {
  val countQuery = "SELECT COUNT(*) FROM ( ${sourceQuery.sql} )"
  val sqLiteQuery: RoomSQLiteQuery = RoomSQLiteQuery.acquire(
    countQuery,
    sourceQuery.argCount
  )
  sqLiteQuery.copyArgumentsFrom(sourceQuery)
  val cursor: Cursor = db.query(sqLiteQuery)
  try {
    if (cursor.moveToFirst()) {
      return cursor.getInt(0)
    }
    return 0
  } finally {
    cursor.close()
    sqLiteQuery.release()
  }
}

/**
 * Returns the key for [PagingSource] for a non-initial REFRESH load.
 *
 * To prevent a negative key, key is clipped to 0 when the number of items available before
 * anchorPosition is less than the requested amount of initialLoadSize / 2.
 */
fun <Value : Any> PagingState<Int, Value>.getClippedRefreshKey(): Int? {
  return when (val anchorPosition = anchorPosition) {
    null -> null
    /**
     *  It is unknown whether anchorPosition represents the item at the top of the screen or item at
     *  the bottom of the screen. To ensure the number of items loaded is enough to fill up the
     *  screen, half of loadSize is loaded before the anchorPosition and the other half is
     *  loaded after the anchorPosition -- anchorPosition becomes the middle item.
     */
    else -> maxOf(0, anchorPosition - (config.initialLoadSize / 2))
  }
}
