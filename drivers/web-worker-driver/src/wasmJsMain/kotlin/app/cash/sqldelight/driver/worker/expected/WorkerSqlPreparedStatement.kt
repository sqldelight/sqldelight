package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.worker.util.add
import app.cash.sqldelight.driver.worker.util.toUint8Array

internal actual class WorkerSqlPreparedStatement : SqlPreparedStatement {

  val parameters = JsArray<JsAny?>()

  actual override fun bindBytes(index: Int, bytes: ByteArray?) {
    parameters.add(bytes?.toUint8Array())
  }

  actual override fun bindLong(index: Int, long: Long?) {
    // We convert Long to Double because Kotlin's Double is mapped to JS number
    // whereas Kotlin's Long is implemented as a JS object
    parameters.add(long?.toDouble()?.toJsNumber())
  }

  actual override fun bindDouble(index: Int, double: Double?) {
    parameters.add(double?.toJsNumber())
  }

  actual override fun bindString(index: Int, string: String?) {
    parameters.add(string?.toJsString())
  }

  actual override fun bindBoolean(index: Int, boolean: Boolean?) {
    parameters.add(boolean?.toJsBoolean())
  }
}
