package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.db.SqlPreparedStatement

internal actual class WorkerSqlPreparedStatement : SqlPreparedStatement {

  val parameters = mutableListOf<Any?>()

  actual override fun bindBytes(index: Int, bytes: ByteArray?) {
    parameters.add(bytes)
  }

  actual override fun bindLong(index: Int, long: Long?) {
    // We convert Long to Double because Kotlin's Double is mapped to JS number
    // whereas Kotlin's Long is implemented as a JS object
    parameters.add(long?.toDouble())
  }

  actual override fun bindDouble(index: Int, double: Double?) {
    parameters.add(double)
  }

  actual override fun bindString(index: Int, string: String?) {
    parameters.add(string)
  }

  actual override fun bindBoolean(index: Int, boolean: Boolean?) {
    parameters.add(boolean)
  }
}
