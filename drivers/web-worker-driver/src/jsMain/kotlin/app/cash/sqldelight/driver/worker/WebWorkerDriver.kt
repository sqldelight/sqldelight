package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.worker.api.WorkerAction
import app.cash.sqldelight.driver.worker.api.WorkerRequest
import app.cash.sqldelight.driver.worker.api.WorkerResponse
import app.cash.sqldelight.driver.worker.api.WorkerResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

/**
 * A [SqlDriver] implementation for interacting with SQL databases running in a Web Worker.
 *
 * This driver is dialect-agnostic and is instead dependent on the Worker script's implementation
 * to handle queries and send results back from the Worker.
 *
 * @property worker The Worker running a SQL implementation that this driver communicates with.
 * @see [WebWorkerDriver.fromScriptUrl]
 */
class WebWorkerDriver(private val worker: Worker) : SqlDriver {
  private val listeners = mutableMapOf<String, MutableSet<Query.Listener>>()
  private var messageCounter = 0
  private var transaction: Transacter.Transaction? = null

  override fun <R> executeQuery(identifier: Int?, sql: String, mapper: (SqlCursor) -> QueryResult<R>, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<R> {
    val bound = JsWorkerSqlPreparedStatement()
    binders?.invoke(bound)

    return QueryResult.AsyncValue {
      val response = worker.sendMessage(WorkerAction.exec) {
        this.sql = sql
        this.params = bound.parameters.toTypedArray()
      }

      return@AsyncValue mapper(JsWorkerSqlCursor(checkWorkerResults(response.results))).await()
    }
  }

  override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> {
    val bound = JsWorkerSqlPreparedStatement()
    binders?.invoke(bound)

    return QueryResult.AsyncValue {
      val response = worker.sendMessage(WorkerAction.exec) {
        this.sql = sql
        this.params = bound.parameters.toTypedArray()
      }
      checkWorkerResults(response.results)
      return@AsyncValue when {
        response.results.values.isEmpty() -> 0L
        else -> response.results.values[0][0].unsafeCast<Double>().toLong()
      }
    }
  }

  override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
    queryKeys.forEach {
      listeners.getOrPut(it) { mutableSetOf() }.add(listener)
    }
  }

  override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
    queryKeys.forEach {
      listeners[it]?.remove(listener)
    }
  }

  override fun notifyListeners(vararg queryKeys: String) {
    queryKeys.flatMap { listeners[it].orEmpty() }
      .distinct()
      .forEach(Query.Listener::queryResultsChanged)
  }

  override fun close() = worker.terminate()

  override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.AsyncValue {
    val enclosing = transaction
    val transaction = Transaction(enclosing)
    this.transaction = transaction
    if (enclosing == null) {
      worker.sendMessage(WorkerAction.beginTransaction)
    }

    return@AsyncValue transaction
  }

  override fun currentTransaction(): Transacter.Transaction? = transaction

  private inner class Transaction(
    override val enclosingTransaction: Transacter.Transaction?,
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean): QueryResult<Unit> = QueryResult.AsyncValue {
      if (enclosingTransaction == null) {
        if (successful) {
          worker.sendMessage(WorkerAction.endTransaction)
        } else {
          worker.sendMessage(WorkerAction.rollbackTransaction)
        }
      }
      transaction = enclosingTransaction
    }
  }

  private suspend fun Worker.sendMessage(action: WorkerAction, message: RequestBuilder.() -> Unit = {}): WorkerResponse = suspendCancellableCoroutine { continuation ->
    val id = messageCounter++
    val messageListener = object : EventListener {
      override fun handleEvent(event: Event) {
        val data = event.unsafeCast<MessageEvent>().data.unsafeCast<WorkerResponse>()
        if (data.id == id) {
          removeEventListener("message", this)
          if (data.error != null) {
            continuation.resumeWithException(WebWorkerException(JSON.stringify(data.error, arrayOf("message", "arguments", "type", "name"))))
          } else {
            continuation.resume(data)
          }
        }
      }
    }

    val errorListener = object : EventListener {
      override fun handleEvent(event: Event) {
        removeEventListener("error", this)
        continuation.resumeWithException(WebWorkerException(JSON.stringify(event, arrayOf("message", "arguments", "type", "name")) + js("Object.entries(event)")))
      }
    }

    addEventListener("message", messageListener)
    addEventListener("error", errorListener)

    val messageObject = js("{}").unsafeCast<WorkerRequest>().apply {
      this.unsafeCast<RequestBuilder>().message()
      this.id = id
      this.action = action
    }

    postMessage(messageObject)

    continuation.invokeOnCancellation {
      removeEventListener("message", messageListener)
      removeEventListener("error", errorListener)
    }
  }

  private fun checkWorkerResults(results: WorkerResult?): Array<Array<dynamic>> {
    checkNotNull(results) { "The worker result was null " }
    check(js("Array.isArray(results.values)").unsafeCast<Boolean>()) { "The worker result values were not an array" }
    return results.values
  }
}

private external interface RequestBuilder {
  var sql: String?
  var params: Array<Any?>?
}

internal class JsWorkerSqlCursor(private val values: Array<Array<dynamic>>) : SqlCursor {
  private var currentRow = -1

  override fun next(): QueryResult.Value<Boolean> = QueryResult.Value(++currentRow < values.size)

  override fun getString(index: Int): String? = values[currentRow][index].unsafeCast<String?>()

  override fun getLong(index: Int): Long? = (values[currentRow][index] as? Double)?.toLong()

  override fun getBytes(index: Int): ByteArray? = (values[currentRow][index] as? Uint8Array)?.let { Int8Array(it.buffer).unsafeCast<ByteArray>() }

  override fun getDouble(index: Int): Double? = values[currentRow][index].unsafeCast<Double?>()

  override fun getBoolean(index: Int): Boolean? = values[currentRow][index].unsafeCast<Boolean?>()
}

internal class JsWorkerSqlPreparedStatement : SqlPreparedStatement {

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
