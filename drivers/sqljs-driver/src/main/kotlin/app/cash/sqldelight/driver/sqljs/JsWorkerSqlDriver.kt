package app.cash.sqldelight.driver.sqljs

import app.cash.sqldelight.AsyncTransacter
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.AsyncSqlDriver
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlPreparedStatement
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import kotlin.js.Promise

private external interface WorkerMessage {
  var id: dynamic
  var action: String
  var sql: String
  var params: Array<Any?>
}

private fun <T> jsObject(block: T.() -> Unit): T {
  val o = js("{}").unsafeCast<T>()
  block(o)
  return o
}

fun initAsyncSqlDriver(workerPath: String = "/worker.sql-wasm.js", schema: AsyncSqlDriver.Schema? = null): Promise<AsyncSqlDriver> {
  val worker = Worker(workerPath)
  return Promise<AsyncSqlDriver> { resolve, _ -> resolve(JsWorkerSqlDriver(worker)) }.withSchema(schema)
}

fun Promise<AsyncSqlDriver>.withSchema(schema: AsyncSqlDriver.Schema? = null): Promise<AsyncSqlDriver> = then {
  schema?.create(it)
  it
}

class JsWorkerSqlDriver(private val worker: Worker) : AsyncSqlDriver {
  private val statements = mutableMapOf<Int, Statement>()
  private val listeners = mutableMapOf<String, MutableSet<Query.Listener>>()
  private var messageCounter = 0

  override fun <R> executeQuery(identifier: Int?, sql: String, mapper: (SqlCursor) -> R, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): AsyncSqlDriver.Callback<R> {
    return AsyncSqlDriver.SimpleCallback<R> { callback ->
      val bound = JsSqlPreparedStatement()
      binders?.invoke(bound)

      val messageId = messageCounter++
      worker.postMessage(
        jsObject<WorkerMessage> {
          this.id = messageId
          this.action = "exec"
          this.sql = sql
          this.params = bound.parameters.toTypedArray()
        }
      )

      worker.addEventListener(
        "message",
        object : EventListener {
          override fun handleEvent(event: Event) {
            check(event is MessageEvent)
            val data = event.data.asDynamic()
            if (data.id == messageId) {
              worker.removeEventListener("message", this)
              if (data.error != null) {
                callback.error(data.error as Throwable)
              }

              try {
                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE") val cursor = JsWorkerSqlCursor(data.results[0].unsafeCast<Table>())
                callback.success(mapper(cursor))
              } catch (e: Exception) {
                callback.error(e)
              }
            }
          }
        }
      )

      worker.addEventListener(
        "error",
        object : EventListener {
          override fun handleEvent(event: Event) {
            worker.removeEventListener("error", this)
            callback.error(event.asDynamic().error as Throwable)
          }
        }
      )
    }
  }

  override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): AsyncSqlDriver.Callback<Long> {
    return AsyncSqlDriver.SimpleCallback { callback ->
      val bound = JsSqlPreparedStatement()
      binders?.invoke(bound)

      val messageId = messageCounter++
      worker.postMessage(
        jsObject<WorkerMessage> {
          this.id = messageId
          this.action = "exec"
          this.sql = sql
          this.params = bound.parameters.toTypedArray()
        }
      )

      worker.addEventListener(
        "message",
        object : EventListener {
          override fun handleEvent(event: Event) {
            check(event is MessageEvent)
            val data = event.data.asDynamic()
            if (data.id == messageId) {
              worker.removeEventListener("message", this)
              if (data.error != null) {
                callback.error(data.error as Throwable)
              }

              try {
                callback.success(0)
              } catch (e: Exception) {
                callback.error(e)
              }
            }
          }
        }
      )

      worker.addEventListener(
        "error",
        object : EventListener {
          override fun handleEvent(event: Event) {
            worker.removeEventListener("error", this)
            callback.error(event.asDynamic().error as Throwable)
          }
        }
      )
    }
  }

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
  }

  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
  }

  override fun notifyListeners(queryKeys: Array<String>) {
  }

  override fun close() = worker.terminate()

  override fun newTransaction(): AsyncSqlDriver.Callback<out AsyncTransacter.Transaction> {
    TODO("Not yet implemented")
  }

  override fun currentTransaction(): AsyncTransacter.Transaction? {
    return null
  }
}

external interface Table {
  val values: Array<Array<dynamic>>
}

class JsWorkerSqlCursor(private val table: Table) : SqlCursor {
  private var currentRow = -1

  override fun next(): Boolean = ++currentRow < table.values.size

  override fun getString(index: Int): String? = table.values[currentRow][index]

  override fun getLong(index: Int): Long? = (table.values[currentRow][index] as? Double)?.toLong()

  override fun getBytes(index: Int): ByteArray? = (table.values[currentRow][index] as? Uint8Array)?.let { Int8Array(it.buffer).unsafeCast<ByteArray>() }

  override fun getDouble(index: Int): Double? = table.values[currentRow][index]

  override fun getBoolean(index: Int): Boolean? {
    val double = (table.values[currentRow][index] as? Double)
    return if (double == null) null
    else double.toLong() == 1L
  }
}

private fun <R> Promise<R>.asCallback(): AsyncSqlDriver.Callback<R> {
  val callback = AsyncSqlDriver.SimpleCallback<R> {}
  then(callback::success)
  catch(callback::error)
  return callback
}

fun <R> AsyncSqlDriver.Callback<R>.asPromise(): Promise<R> = Promise { resolve, reject ->
  onSuccess { resolve(it) }
  onError { reject(it) }
  start()
}
