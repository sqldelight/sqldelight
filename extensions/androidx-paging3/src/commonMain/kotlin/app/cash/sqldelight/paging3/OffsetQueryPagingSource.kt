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

import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadParamsAppend
import app.cash.paging.PagingSourceLoadParamsPrepend
import app.cash.paging.PagingSourceLoadParamsRefresh
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultInvalid
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
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
    params: PagingSourceLoadParams<Int>,
  ): PagingSourceLoadResult<Int, RowType> = withContext(context) {
    val key = params.key ?: initialOffset
    val limit = when (params) {
      is PagingSourceLoadParamsPrepend<*> -> minOf(key, params.loadSize)
      else -> params.loadSize
    }
    val getPagingSourceLoadResult: TransactionCallbacks.() -> PagingSourceLoadResultPage<Int, RowType> = {
      val count = countQuery.executeAsOne()
      val offset = when (params) {
        is PagingSourceLoadParamsPrepend<*> -> maxOf(0, key - params.loadSize)
        is PagingSourceLoadParamsAppend<*> -> key
        is PagingSourceLoadParamsRefresh<*> -> if (key >= count) maxOf(0, count - params.loadSize) else key
        else -> error("Unknown PagingSourceLoadParams ${params::class}")
      }
      val data = queryProvider(limit, offset)
        .also { currentQuery = it }
        .executeAsList()
      val nextPosToLoad = offset + data.size
      PagingSourceLoadResultPage(
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
    (if (invalid) PagingSourceLoadResultInvalid<Int, RowType>() else loadResult) as PagingSourceLoadResult<Int, RowType>
  }

  override fun getRefreshKey(state: PagingState<Int, RowType>) =
    state.anchorPosition?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }
}
