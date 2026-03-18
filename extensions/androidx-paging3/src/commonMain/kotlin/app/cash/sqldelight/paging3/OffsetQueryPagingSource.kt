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
import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterBase
import app.cash.sqldelight.TransactionCallbacks
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

internal class OffsetQueryPagingSource<RowType : Any>(
  private val queryProvider: (limit: Int, offset: Int) -> Query<RowType>,
  private val countQuery: Query<Int>,
  private val transacter: TransacterBase,
  private val context: CoroutineContext,
  private val initialOffset: Int,
) : QueryPagingSource<Int, RowType>() {

  override val jumpingSupported get() = true

  override suspend fun load(
    params: LoadParams<Int>,
  ): LoadResult<Int, RowType> = withContext(context) {
    val key = params.key ?: initialOffset
    val limit = when (params) {
      is LoadParams.Prepend -> minOf(key, params.loadSize)
      else -> params.loadSize
    }
    val getPagingSourceLoadResult: TransactionCallbacks.() -> LoadResult.Page<Int, RowType> = {
      val count = countQuery.executeAsOne()
      val offset = when (params) {
        is LoadParams.Prepend -> maxOf(0, key - params.loadSize)
        is LoadParams.Append -> key
        is LoadParams.Refresh -> if (key >= count - params.loadSize) maxOf(0, count - params.loadSize) else key
      }
      val data = queryProvider(limit, offset)
        .also { currentQuery = it }
        .executeAsList()
      val nextPosToLoad = offset + data.size
      LoadResult.Page(
        data = data,
        prevKey = offset.takeIf { it > 0 && data.isNotEmpty() },
        nextKey = nextPosToLoad.takeIf { data.isNotEmpty() && data.size >= limit && it < count },
        itemsBefore = offset,
        itemsAfter = maxOf(0, count - nextPosToLoad),
      )
    }
    val loadResult = when (transacter) {
      is Transacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
      is SuspendingTransacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
    }
    (if (invalid) LoadResult.Invalid() else loadResult)
  }

  override fun getRefreshKey(state: PagingState<Int, RowType>) = state.anchorPosition?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }
}
