package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.worker.util.add
import app.cash.sqldelight.driver.worker.util.toUint8Array

internal actual class JsWorkerSqlPreparedStatement : SqlPreparedStatement {

  val parameters = JsArray<JsAny?>()

  override fun bindBytes(index: Int, bytes: ByteArray?) {
    parameters.add(bytes?.toUint8Array())
  }

  override fun bindLong(index: Int, long: Long?) {
    // We convert Long to Double because Kotlin's Double is mapped to JS number
    // whereas Kotlin's Long is implemented as a JS object
    parameters.add(long?.toDouble()?.toJsNumber())
  }

  override fun bindDouble(index: Int, double: Double?) {
    parameters.add(double?.toJsNumber())
  }

  override fun bindString(index: Int, string: String?) {
    parameters.add(string?.toJsString())
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    parameters.add(boolean?.toJsBoolean())
  }
}
