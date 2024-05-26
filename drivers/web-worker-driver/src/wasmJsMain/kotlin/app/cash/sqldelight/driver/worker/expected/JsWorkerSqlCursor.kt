package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.worker.api.WorkerResult
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

internal actual class JsWorkerSqlCursor actual constructor(
  private val result: WorkerResult,
) : SqlCursor {
  private var currentRow = -1
  private val values: JsArray<JsArray<JsAny>> by lazy {
    result.values!!
  }

  override fun next(): QueryResult.Value<Boolean> = QueryResult.Value(++currentRow < values.length)

  override fun getString(index: Int): String? {
    val currentRow = values[currentRow] ?: return null
    return currentRow[index]?.unsafeCast<JsString>()?.toString()
  }

  override fun getLong(index: Int): Long? {
    return getColumn(index) {
      it.unsafeCast<JsNumber>().toDouble().toLong()
    }
  }

  override fun getBytes(index: Int): ByteArray? {
    return getColumn(index) {
      val array = it.unsafeCast<Uint8Array>()
      // TODO: avoid copying somehow?
      ByteArray(array.length) { array[it] }
    }
  }

  override fun getDouble(index: Int): Double? {
    return getColumn(index) { it.unsafeCast<JsNumber>().toDouble() }
  }

  override fun getBoolean(index: Int): Boolean? {
    return getColumn(index) { it.unsafeCast<JsBoolean>().toBoolean() }
  }

  private inline fun <T> getColumn(index: Int, transformer: (JsAny) -> T): T? {
    val column = values[currentRow]?.get(index) ?: return null
    return transformer(column)
  }
}
