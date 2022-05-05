package app.cash.sqldelight.driver.sqljs

import app.cash.sqldelight.async.AsyncQuery
import app.cash.sqldelight.async.AsyncTransacter
import app.cash.sqldelight.async.db.AsyncSqlCursor
import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.async.db.AsyncSqlPreparedStatement
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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

suspend fun initAsyncSqlDriver(workerPath: String = "/worker.sql-wasm.js", schema: AsyncSqlDriver.Schema? = null): AsyncSqlDriver {
  val worker = Worker(workerPath)
  return Promise<AsyncSqlDriver> { resolve, _ -> resolve(JsWorkerSqlDriver(worker)) }.withSchema(schema)
}

suspend fun Promise<AsyncSqlDriver>.withSchema(schema: AsyncSqlDriver.Schema? = null): AsyncSqlDriver = await().also { schema?.create(it) }

class JsWorkerSqlDriver(private val worker: Worker) : AsyncSqlDriver {
  private val statements = mutableMapOf<Int, Statement>()
  private val listeners = mutableMapOf<String, MutableSet<AsyncQuery.Listener>>()
  private var messageCounter = 0

  override suspend fun <R> executeQuery(identifier: Int?, sql: String, mapper: (AsyncSqlCursor) -> R, parameters: Int, binders: (AsyncSqlPreparedStatement.() -> Unit)?): R =
    suspendCancellableCoroutine { continuation ->
      val bound = JsWorkerSqlPreparedStatement()
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

      val messageListener = object : EventListener {
        override fun handleEvent(event: Event) {
          check(event is MessageEvent)
          val data = event.data.asDynamic()
          if (data.id == messageId) {
            worker.removeEventListener("message", this)
            if (data.error != null) {
              continuation.resumeWithException(data.error as Throwable)
            }

            val table = if (data.results.length as Int > 0) {
              data.results[0].unsafeCast<Table>()
            } else {
              jsObject { values = arrayOf() }
            }
            try {
              @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE") val cursor = JsWorkerSqlCursor(table)
              continuation.resume(mapper(cursor))
            } catch (e: Exception) {
              continuation.resumeWithException(e)
            }
          }
        }
      }

      val errorListener = object : EventListener {
        override fun handleEvent(event: Event) {
          worker.removeEventListener("error", this)
          continuation.resumeWithException(event.asDynamic().error as Throwable)
        }
      }

      worker.addEventListener("message", messageListener)
      worker.addEventListener("error", errorListener)

      continuation.invokeOnCancellation {
        worker.removeEventListener("message", messageListener)
        worker.removeEventListener("error", errorListener)
      }
    }

  override suspend fun execute(identifier: Int?, sql: String, parameters: Int, binders: (AsyncSqlPreparedStatement.() -> Unit)?): Long {
    return suspendCancellableCoroutine { continuation ->
      val bound = JsWorkerSqlPreparedStatement()
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

      val messageListener = object : EventListener {
        override fun handleEvent(event: Event) {
          check(event is MessageEvent)
          val data = event.data.asDynamic()
          if (data.id == messageId) {
            worker.removeEventListener("message", this)
            if (data.error != null) {
              continuation.resumeWithException(data.error as Throwable)
            }

            try {
              continuation.resume(0)
            } catch (e: Exception) {
              continuation.resumeWithException(e)
            }
          }
        }
      }

      val errorListener = object : EventListener {
        override fun handleEvent(event: Event) {
          worker.removeEventListener("error", this)
          continuation.resumeWithException(event.asDynamic().error as Throwable)
        }
      }

      worker.addEventListener("message", messageListener)
      worker.addEventListener("error", errorListener)

      continuation.invokeOnCancellation {
        worker.removeEventListener("message", messageListener)
        worker.removeEventListener("error", errorListener)
      }
    }
  }

  override fun addListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) {
  }

  override fun removeListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) {
  }

  override fun notifyListeners(queryKeys: Array<String>) {
  }

  override suspend fun close() = worker.terminate()

  override suspend fun newTransaction(): AsyncTransacter.Transaction {
    TODO("Not yet implemented")
  }

  override fun currentTransaction(): AsyncTransacter.Transaction? {
    return null
  }
}

external interface Table {
  var values: Array<Array<dynamic>>
}

class JsWorkerSqlCursor(private val table: Table) : AsyncSqlCursor {
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

internal class JsWorkerSqlPreparedStatement : AsyncSqlPreparedStatement {

  val parameters = mutableListOf<Any?>()

  override fun bindBytes(index: Int, bytes: ByteArray?) {
    parameters.add(bytes?.toTypedArray())
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
    parameters.add(
      when (boolean) {
        null -> null
        true -> 1.0
        false -> 0.0
      }
    )
  }
}
