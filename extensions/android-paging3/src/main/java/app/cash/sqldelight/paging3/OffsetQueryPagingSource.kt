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
import app.cash.sqldelight.Transacter
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class OffsetQueryPagingSource<RowType : Any>(
  private val queryProvider: (limit: Int, offset: Int) -> Query<RowType>,
  private val countQuery: Query<Int>,
  private val transacter: Transacter,
  private val context: CoroutineContext,
) : QueryPagingSource<Int, RowType>() {

  override val jumpingSupported get() = true

  override suspend fun load(
    params: LoadParams<Int>,
  ): LoadResult<Int, RowType> = withContext(context) {
    try {
      val key = params.key ?: 0
      transacter.transactionWithResult {
        val count = countQuery.executeAsOne()
        if (count != 0 && key >= count) throw IndexOutOfBoundsException()

        val loadSize = if (key < 0) params.loadSize + key else params.loadSize

        val data = queryProvider(loadSize, maxOf(0, key))
          .also { currentQuery = it }
          .executeAsList()

        LoadResult.Page(
          data = data,
          // allow one, and only one negative prevKey in a paging set. This is done for
          // misaligned prepend queries to avoid duplicates.
          prevKey = if (key <= 0L) null else key - params.loadSize,
          nextKey = if (key + params.loadSize >= count) null else key + params.loadSize,
          itemsBefore = maxOf(0, key),
          itemsAfter = maxOf(0, (count - (key + params.loadSize))),
        )
      }
    } catch (e: Exception) {
      if (e is IndexOutOfBoundsException) throw e
      LoadResult.Error(e)
    }
  }

  override fun getRefreshKey(state: PagingState<Int, RowType>) = state.anchorPosition
}
