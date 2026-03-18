package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.db.SqlPreparedStatement

internal expect class WorkerSqlPreparedStatement() : SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?)

  override fun bindLong(index: Int, long: Long?)

  override fun bindDouble(index: Int, double: Double?)

  override fun bindString(index: Int, string: String?)

  override fun bindBoolean(index: Int, boolean: Boolean?)
}
