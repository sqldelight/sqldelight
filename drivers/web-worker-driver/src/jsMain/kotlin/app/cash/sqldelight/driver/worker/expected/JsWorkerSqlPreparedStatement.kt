package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.db.SqlPreparedStatement

internal actual class JsWorkerSqlPreparedStatement : SqlPreparedStatement {

  val parameters = mutableListOf<Any?>()

  override fun bindBytes(index: Int, bytes: ByteArray?) {
    parameters.add(bytes)
  }

  override fun bindLong(index: Int, long: Long?) {
    // We convert Long to Double because Kotlin's Double is mapped to JS number
    // whereas Kotlin's Long is implemented as a JS object
    parameters.add(long?.toDouble())
  }

  override fun bindDouble(index: Int, double: Double?) {
    parameters.add(double)
  }

  override fun bindString(index: Int, string: String?) {
    parameters.add(string)
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    parameters.add(boolean)
  }
}
