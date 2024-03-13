package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.worker.api.WorkerAction
import app.cash.sqldelight.driver.worker.api.WorkerActions
import app.cash.sqldelight.driver.worker.api.WorkerRequest
import app.cash.sqldelight.driver.worker.api.WorkerResponse
import app.cash.sqldelight.driver.worker.api.WorkerResult
import app.cash.sqldelight.driver.worker.util.add
import app.cash.sqldelight.driver.worker.util.instantiateObject
import app.cash.sqldelight.driver.worker.util.isArray
import app.cash.sqldelight.driver.worker.util.jsonStringify
import app.cash.sqldelight.driver.worker.util.objectEntries
import app.cash.sqldelight.driver.worker.util.toJsArray
import app.cash.sqldelight.driver.worker.util.toUint8Array
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event

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
      val response = worker.sendMessage(WorkerActions.exec) {
        this.sql = sql
        this.params = bound.parameters
      }

      return@AsyncValue mapper(JsWorkerSqlCursor(checkWorkerResults(response.results))).await()
    }
  }

  override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> {
    val bound = JsWorkerSqlPreparedStatement()
    binders?.invoke(bound)

    return QueryResult.AsyncValue {
      val response = worker.sendMessage(WorkerActions.exec) {
        this.sql = sql
        this.params = bound.parameters
      }
      val values = checkWorkerResults(response.results)
      return@AsyncValue when {
        values.length == 0 -> 0L
        else -> values[0]?.get(0)?.unsafeCast<JsNumber>()?.toDouble()?.toLong() ?: 0L
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
      worker.sendMessage(WorkerActions.beginTransaction)
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
          worker.sendMessage(WorkerActions.endTransaction)
        } else {
          worker.sendMessage(WorkerActions.rollbackTransaction)
        }
      }
      transaction = enclosingTransaction
    }
  }

  private suspend fun Worker.sendMessage(action: WorkerAction, message: RequestBuilder.() -> Unit = {}): WorkerResponse = suspendCancellableCoroutine { continuation ->
    val id = messageCounter++
    var messageListener: ((Event) -> Unit)? = null
    messageListener = { event: Event ->
      val message = event.unsafeCast<MessageEvent>()
      val data = message.data?.unsafeCast<WorkerResponse>()
      if (data == null) {
        continuation.resumeWithException(WebWorkerException("Message ${message.type} data was null or not a WorkerResponse"))
      } else {
        if (data.id == id) {
          removeEventListener("message", messageListener)
          if (data.error != null) {
            continuation.resumeWithException(WebWorkerException(jsonStringify(value = data.error, replacer = listOf("message", "arguments", "type", "name").toJsArray { it.toJsString() })))
          } else {
            continuation.resume(data)
          }
        }
      }
    }
    var errorListener: ((Event) -> Unit)? = null
    errorListener = { event ->
      removeEventListener("error", errorListener)
      continuation.resumeWithException(WebWorkerException(jsonStringify(event, listOf("message", "arguments", "type", "name").toJsArray { it.toJsString() }) + objectEntries(event)))
    }
    addEventListener("message", messageListener)
    addEventListener("error", errorListener)

    val messageObject = instantiateObject<WorkerRequest>().apply {
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

  private fun checkWorkerResults(results: WorkerResult?): JsArray<JsArray<JsAny>> {
    checkNotNull(results) { "The worker result was null " }
    val values = results.values
    check(values != null && isArray(values)) { "The worker result values were not an array" }
    return values
  }
}

private external interface RequestBuilder : JsAny {
  var sql: String?
  var params: JsArray<JsAny?>?
}

internal class JsWorkerSqlCursor(private val values: JsArray<JsArray<JsAny>>) : SqlCursor {
  private var currentRow = -1

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

internal class JsWorkerSqlPreparedStatement : SqlPreparedStatement {

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
