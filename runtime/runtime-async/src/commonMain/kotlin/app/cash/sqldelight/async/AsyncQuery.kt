package app.cash.sqldelight.async

import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.async.db.AsyncSqlCursor

@Suppress("FunctionName") // Emulating a constructor.
fun <RowType : Any> AsyncQuery(
        identifier: Int,
        queryKeys: Array<String>,
        driver: AsyncSqlDriver,
        query: String,
        mapper: (AsyncSqlCursor) -> RowType
): AsyncQuery<RowType> {
  return AsyncQuery(identifier, queryKeys, driver, "unknown", "unknown", query, mapper)
}

@Suppress("FunctionName") // Emulating a constructor.
fun <RowType : Any> AsyncQuery(
        identifier: Int,
        queryKeys: Array<String>,
        driver: AsyncSqlDriver,
        fileName: String,
        label: String,
        query: String,
        mapper: (AsyncSqlCursor) -> RowType
): AsyncQuery<RowType> {
  return SimpleAsyncQuery(identifier, queryKeys, driver, fileName, label, query, mapper)
}

private class SimpleAsyncQuery<out RowType : Any>(
        private val identifier: Int,
        private val queryKeys: Array<String>,
        private val driver: AsyncSqlDriver,
        private val fileName: String,
        private val label: String,
        private val query: String,
        mapper: (AsyncSqlCursor) -> RowType,
) : AsyncQuery<RowType>(mapper) {
  override suspend fun <R> execute(mapper: (AsyncSqlCursor) -> R): R {
    return driver.executeQuery(identifier, query, mapper, 0, null)
  }

  override fun toString(): String = "$fileName:$label"

  override fun addListener(listener: Listener) {
    driver.addListener(listener, queryKeys)
  }

  override fun removeListener(listener: Listener) {
    driver.removeListener(listener, queryKeys)
  }
}

private class SimpleAsyncExecutableQuery<out RowType : Any>(
        private val identifier: Int,
        private val driver: AsyncSqlDriver,
        private val fileName: String,
        private val label: String,
        private val query: String,
        mapper: (AsyncSqlCursor) -> RowType
) : AsyncExecutableQuery<RowType>(mapper) {
  override suspend fun <R> execute(mapper: (AsyncSqlCursor) -> R): R {
    return driver.executeQuery(identifier, query, mapper, 0, null)
  }

  override fun toString(): String = "$fileName:$label"
}

abstract class AsyncExecutableQuery<out RowType : Any>(
  val mapper: (AsyncSqlCursor) -> RowType
) {
  abstract suspend fun <R> execute(mapper: (AsyncSqlCursor) -> R): R

  suspend fun executeAsList(): List<RowType> = execute { cursor ->
    val result = mutableListOf<RowType>()
    while (cursor.next()) result.add(mapper(cursor))
    result
  }

  suspend fun executeAsOne(): RowType {
    return executeAsOneOrNull()
            ?: throw NullPointerException("ResultSet returned null for $this")
  }

  suspend fun executeAsOneOrNull(): RowType? = execute { cursor ->
    if (!cursor.next()) return@execute null
    val value = mapper(cursor)
    check(!cursor.next()) { "ResultSet returned more than 1 row for $this" }
    value
  }
}

abstract class AsyncQuery<out RowType : Any>(
  mapper: (AsyncSqlCursor) -> RowType
) : AsyncExecutableQuery<RowType>(mapper) {
  /**
   * Register a listener to be notified of future changes in the result set.
   */
  abstract fun addListener(listener: Listener)

  /**
   * Remove a listener to no longer be notified of future changes in the result set.
   */
  abstract fun removeListener(listener: Listener)

  /**
   * An interface for listening to changes in the result set of a query.
   */
  interface Listener {
    /**
     * Called whenever the query this listener was attached to is dirtied.
     *
     * Calls are made synchronously on the thread where the updated occurred, after the update applied successfully.
     */
    fun queryResultsChanged()
  }
}
