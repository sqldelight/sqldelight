package com.squareup.sqldelight.android.paging

import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter

class QueryDataSourceFactory<RowType : Any>(
  private val queryProvider: (limit: Long, offset: Long) -> Query<RowType>,
  private val countQuery: Query<Long>,
  private val transacter: Transacter
) : DataSource.Factory<Int, RowType>() {
  override fun create(): PositionalDataSource<RowType> =
    QueryDataSource(queryProvider, countQuery, transacter)
}

private class QueryDataSource<RowType : Any>(
  private val queryProvider: (limit: Long, offset: Long) -> Query<RowType>,
  private val countQuery: Query<Long>,
  private val transacter: Transacter
) : PositionalDataSource<RowType>(),
    Query.Listener {
  private var query: Query<RowType>? = null

  override fun queryResultsChanged() = invalidate()

  override fun loadRange(
    params: LoadRangeParams,
    callback: LoadRangeCallback<RowType>
  ) {
    query?.removeListener(this)
    queryProvider(params.loadSize.toLong(), params.startPosition.toLong()).let { query ->
      query.addListener(this)
      this.query = query
      if (!isInvalid) {
        callback.onResult(query.executeAsList())
      }
    }
  }

  override fun loadInitial(
    params: LoadInitialParams,
    callback: LoadInitialCallback<RowType>
  ) {
    query?.removeListener(this)
    queryProvider(params.requestedLoadSize.toLong(), params.requestedStartPosition.toLong()).let { query ->
      query.addListener(this)
      this.query = query
      if (!isInvalid) {
        transacter.transaction {
          callback.onResult(
              /* data = */ query.executeAsList(),
              /* position = */ params.requestedStartPosition,
              /* totalCount = */ countQuery.executeAsOne().toInt()
          )
        }
      }
    }
  }
}
