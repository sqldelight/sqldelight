package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.api.WorkerResult
import app.cash.sqldelight.driver.worker.api.WorkerResponse
import app.cash.sqldelight.driver.worker.api.WorkerRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private fun <T> jsObject(block: T.() -> Unit): T {
  val o = js("{}").unsafeCast<T>()
  block(o)
  return o
}

suspend fun initAsyncSqlDriver(
  worker: Worker,
  schema: SqlSchema? = null,
): SqlDriver = JsWorkerSqlDriver(worker).withSchema(schema)

suspend fun SqlDriver.withSchema(schema: SqlSchema? = null): SqlDriver = this.also { schema?.create(it)?.await() }

class JsWorkerSqlDriver(private val worker: Worker) : SqlDriver {
  private val listeners = mutableMapOf<String, MutableSet<Query.Listener>>()
  private var messageCounter = 0
  private var transaction: Transacter.Transaction? = null

  override fun <R> executeQuery(identifier: Int?, sql: String, mapper: (SqlCursor) -> R, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<R> {
    val bound = JsWorkerSqlPreparedStatement()
    binders?.invoke(bound)

    return QueryResult.AsyncValue {
      val response = worker.sendMessage(Action.EXEC) {
        this.sql = sql
        this.params = bound.parameters.toTypedArray()
      }

      return@AsyncValue mapper(JsWorkerSqlCursor(checkNotNull(response.result) { "The worker result was null" }))
    }
  }

  override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> {
    val bound = JsWorkerSqlPreparedStatement()
    binders?.invoke(bound)

    return QueryResult.AsyncValue {
      val response = worker.sendMessage(Action.EXEC) {
        this.sql = sql
        this.params = bound.parameters.toTypedArray()
      }
      val result = checkNotNull(response.result) { "The worker result was null" }
      return@AsyncValue (if (result.values.isEmpty()) { 0L } else { result.values[0][0].unsafeCast<Double>().toLong() })
    }
  }

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
    queryKeys.forEach {
      listeners.getOrPut(it) { mutableSetOf() }.add(listener)
    }
  }

  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
    queryKeys.forEach {
      listeners[it]?.remove(listener)
    }
  }

  override fun notifyListeners(queryKeys: Array<String>) {
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
      worker.sendMessage(Action.BEGIN_TRANSACTION)
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
          worker.sendMessage(Action.END_TRANSACTION)
        } else {
          worker.sendMessage(Action.ROLLBACK_TRANSACTION)
        }
      }
      transaction = enclosingTransaction
    }
  }

  private suspend fun Worker.sendMessage(action: Action, message: RequestBuilder.() -> Unit = {}): WorkerResponse = suspendCancellableCoroutine { continuation ->
    val id = messageCounter++
    val messageListener = object : EventListener {
      override fun handleEvent(event: Event) {
        val data = event.unsafeCast<MessageEvent>().data.unsafeCast<WorkerResponse>()
        if (data.id == id) {
          removeEventListener("message", this)
          if (data.error != null) {
            continuation.resumeWithException(JsWorkerException(JSON.stringify(data.error, arrayOf("message", "arguments", "type", "name"))))
          } else {
            continuation.resume(data)
          }
        }
      }
    }

    val errorListener = object : EventListener {
      override fun handleEvent(event: Event) {
        removeEventListener("error", this)
        continuation.resumeWithException(JsWorkerException(JSON.stringify(event, arrayOf("message", "arguments", "type", "name")) + js("Object.entries(event)")))
      }
    }

    addEventListener("message", messageListener)
    addEventListener("error", errorListener)

    postMessage(jsObject<WorkerRequest> {
      this.unsafeCast<RequestBuilder>().message()
      this.id = id
      this.action = action.key
    })

    continuation.invokeOnCancellation {
      removeEventListener("message", messageListener)
      removeEventListener("error", errorListener)
    }
  }

  /**
   * An enum mapping of [WorkerRequest.action]
   */
  private enum class Action(val key: String) {
    EXEC("exec"),
    BEGIN_TRANSACTION("begin_transaction"),
    END_TRANSACTION("end_transaction"),
    ROLLBACK_TRANSACTION("rollback_transaction"),
  }
}

private external interface RequestBuilder {
  var sql: String?
  var params: Array<Any?>?
}

class JsWorkerSqlCursor(private val table: WorkerResult) : SqlCursor {
  private var currentRow = -1

  override fun next(): Boolean = ++currentRow < table.values.size

  override fun getString(index: Int): String? = table.values[currentRow][index].unsafeCast<String?>()

  override fun getLong(index: Int): Long? = (table.values[currentRow][index] as? Double)?.toLong()

  override fun getBytes(index: Int): ByteArray? = (table.values[currentRow][index] as? Uint8Array)?.let { Int8Array(it.buffer).unsafeCast<ByteArray>() }

  override fun getDouble(index: Int): Double? = table.values[currentRow][index].unsafeCast<Double?>()

  override fun getBoolean(index: Int): Boolean? = table.values[currentRow][index].unsafeCast<Boolean?>()
}

internal class JsWorkerSqlPreparedStatement : SqlPreparedStatement {

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
    parameters.add(boolean)
  }
}
