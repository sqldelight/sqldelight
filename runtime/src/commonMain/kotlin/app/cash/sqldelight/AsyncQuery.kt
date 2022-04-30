package app.cash.sqldelight

import app.cash.sqldelight.db.AsyncSqlDriver
import app.cash.sqldelight.db.SqlCursor

@Suppress("FunctionName") // Emulating a constructor.
fun <RowType : Any> AsyncQuery(
  identifier: Int,
  queryKeys: Array<String>,
  driver: AsyncSqlDriver,
  query: String,
  mapper: (SqlCursor) -> RowType
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
  mapper: (SqlCursor) -> RowType
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
  mapper: (SqlCursor) -> RowType,
) : AsyncQuery<RowType>(mapper) {
  override fun <R> execute(onSuccess: (R) -> Unit, onError: (Throwable) -> Unit, mapper: (SqlCursor) -> R): AsyncSqlDriver.Callback<R> {
    return driver.executeQuery(identifier, query, mapper, 0, null).also {
      it.onSuccess(onSuccess)
      it.onError(onError)
    }
  }

  override fun toString(): String = "$fileName:$label"

  override fun addListener(listener: Query.Listener) {
    driver.addListener(listener, queryKeys)
  }

  override fun removeListener(listener: Query.Listener) {
    driver.removeListener(listener, queryKeys)
  }
}

private class SimpleAsyncExecutableQuery<out RowType : Any>(
  private val identifier: Int,
  private val driver: AsyncSqlDriver,
  private val fileName: String,
  private val label: String,
  private val query: String,
  mapper: (SqlCursor) -> RowType
) : AsyncExecutableQuery<RowType>(mapper) {
  override fun <R> execute(onSuccess: (R) -> Unit, onError: (Throwable) -> Unit, mapper: (SqlCursor) -> R): AsyncSqlDriver.Callback<R> {
    return driver.executeQuery(identifier, query, mapper, 0, null).also {
      it.onSuccess(onSuccess)
      it.onError(onError)
    }
  }

  override fun toString(): String = "$fileName:$label"
}

abstract class AsyncExecutableQuery<out RowType : Any>(
  val mapper: (SqlCursor) -> RowType
) {
  abstract fun <R> execute(onSuccess: (R) -> Unit, onError: (Throwable) -> Unit, mapper: (SqlCursor) -> R): AsyncSqlDriver.Callback<R>

  fun executeAsList(onSuccess: (List<RowType>) -> Unit = {}, onError: (Throwable) -> Unit = {}): AsyncSqlDriver.Callback<out List<RowType>> = execute(onSuccess, onError) { cursor ->
    val result = mutableListOf<RowType>()
    while (cursor.next()) result.add(mapper(cursor))
    result
  }

  // TODO: Extract common code?
  fun executeAsOne(onSuccess: (RowType) -> Unit = {}, onError: (Throwable) -> Unit = {}): AsyncSqlDriver.Callback<out RowType> = execute(onSuccess, onError) { cursor ->
    if (!cursor.next()) throw NullPointerException("ResultSet returned null for $this")
    val value = mapper(cursor)
    check(!cursor.next()) { "ResultSet returned more than 1 row for $this" }
    value
  }

  fun executeAsOneOrNull(onSuccess: (RowType?) -> Unit = {}, onError: (Throwable) -> Unit = {}): AsyncSqlDriver.Callback<out RowType?> = execute(onSuccess, onError) { cursor ->
    if (!cursor.next()) return@execute null
    val value = mapper(cursor)
    check(!cursor.next()) { "ResultSet returned more than 1 row for $this" }
    value
  }
}

abstract class AsyncQuery<out RowType : Any>(
  mapper: (SqlCursor) -> RowType
) : AsyncExecutableQuery<RowType>(mapper) {
  /**
   * Register a listener to be notified of future changes in the result set.
   */
  abstract fun addListener(listener: Query.Listener)

  /**
   * Remove a listener to no longer be notified of future changes in the result set.
   */
  abstract fun removeListener(listener: Query.Listener)
}
