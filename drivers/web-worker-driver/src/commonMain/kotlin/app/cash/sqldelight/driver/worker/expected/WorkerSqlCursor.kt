package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.worker.api.WorkerResult

internal expect class WorkerSqlCursor(result: WorkerResult) : SqlCursor {
  override fun next(): QueryResult<Boolean>

  override fun getString(index: Int): String?

  override fun getLong(index: Int): Long?

  override fun getBytes(index: Int): ByteArray?

  override fun getDouble(index: Int): Double?

  override fun getBoolean(index: Int): Boolean?
}
