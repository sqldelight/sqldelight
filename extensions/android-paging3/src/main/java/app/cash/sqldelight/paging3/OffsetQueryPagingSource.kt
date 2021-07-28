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

import androidx.paging.PagingState
import app.cash.sqldelight.Query
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class OffsetQueryPagingSource<RowType : Any>(
  private val queryProvider: (limit: Long, offset: Long) -> Query<RowType>,
  private val countQuery: Query<Long>,
  private val context: CoroutineContext,
) : QueryPagingSource<Long, RowType>() {

  override val jumpingSupported get() = true
  private var cachedCount: Long? = null

  override suspend fun load(
    params: LoadParams<Long>,
  ): LoadResult<Long, RowType> = withContext(context) {
    try {
      val count = cachedCount ?: countQuery.executeAsOne().also { cachedCount = it }
      val key = when (params) {
        is LoadParams.Refresh -> params.key?.coerceIn(
          minimumValue = 0L,
          // coerce this key so that the max value would be a key
          // that has a full page refresh
          maximumValue = count - params.loadSize
        )
        else -> params.key
      } ?: 0L

      if (count != 0L && key >= count) throw IndexOutOfBoundsException()

      val loadSize = if (key < 0) params.loadSize + key else params.loadSize

      val data = queryProvider(loadSize.toLong(), maxOf(0, key))
        .also { currentQuery = it }
        .executeAsList()

      LoadResult.Page(
        data = data,
        // allow one, and only one negative prevKey in a paging set. This is done for
        // misaligned prepend queries to avoid duplicates.
        prevKey = if (key <= 0L) null else key - params.loadSize,
        nextKey = if (key + params.loadSize >= count) null else key + params.loadSize,
        itemsBefore = maxOf(0L, key).toInt(),
        itemsAfter = maxOf(0, (count - (key + params.loadSize))).toInt()
      )
    } catch (e: Exception) {
      if (e is IndexOutOfBoundsException) throw e
      LoadResult.Error(e)
    }
  }

  override fun getRefreshKey(state: PagingState<Long, RowType>) = state.anchorPosition?.toLong()
}
