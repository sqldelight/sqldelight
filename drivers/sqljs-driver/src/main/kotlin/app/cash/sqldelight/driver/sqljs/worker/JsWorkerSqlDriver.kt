package app.cash.sqldelight.driver.sqljs.worker

import app.cash.sqldelight.async.AsyncQuery
import app.cash.sqldelight.async.AsyncTransacter
import app.cash.sqldelight.async.db.AsyncSqlCursor
import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.async.db.AsyncSqlPreparedStatement
import app.cash.sqldelight.async.db.AsyncSqlSchema
import app.cash.sqldelight.driver.sqljs.QueryResults
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
  workerPath: String = "/worker.sql-wasm.js",
  schema: AsyncSqlSchema? = null
): AsyncSqlDriver = JsWorkerSqlDriver(Worker(workerPath)).withSchema(schema)

suspend fun AsyncSqlDriver.withSchema(schema: AsyncSqlSchema? = null): AsyncSqlDriver = this.also { schema?.create(it) }

class JsWorkerSqlDriver(private val worker: Worker) : AsyncSqlDriver {
  private val listeners = mutableMapOf<String, MutableSet<AsyncQuery.Listener>>()
  private var messageCounter = 0
  private var transaction: AsyncTransacter.Transaction? = null

  override suspend fun <R> executeQuery(identifier: Int?, sql: String, mapper: (AsyncSqlCursor) -> R, parameters: Int, binders: (AsyncSqlPreparedStatement.() -> Unit)?): R {
    val bound = JsWorkerSqlPreparedStatement()
    binders?.invoke(bound)

    val messageId = messageCounter++
    val message = jsObject<WorkerMessage> {
      this.id = messageId
      this.action = "exec"
      this.sql = sql
      this.params = bound.parameters.toTypedArray()
    }

    val data = worker.sendMessage(messageId, message).unsafeCast<WorkerData>()

    val table = if (data.results.isNotEmpty()) {
      data.results[0]
    } else {
      jsObject { values = arrayOf() }
    }

    return mapper(JsWorkerSqlCursor(table))
  }

  override suspend fun execute(identifier: Int?, sql: String, parameters: Int, binders: (AsyncSqlPreparedStatement.() -> Unit)?): Long {
    val bound = JsWorkerSqlPreparedStatement()
    binders?.invoke(bound)

    val messageId = messageCounter++
    val message = jsObject<WorkerMessage> {
      this.id = messageId
      this.action = "exec"
      this.sql = sql
      this.params = bound.parameters.toTypedArray()
    }

    worker.sendMessage(messageId, message)
    return 0
  }

  override fun addListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) {
    queryKeys.forEach {
      listeners.getOrPut(it) { mutableSetOf() }.add(listener)
    }
  }

  override fun removeListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) {
    queryKeys.forEach {
      listeners[it]?.remove(listener)
    }
  }

  override fun notifyListeners(queryKeys: Array<String>) {
    queryKeys.flatMap { listeners[it].orEmpty() }
      .distinct()
      .forEach(AsyncQuery.Listener::queryResultsChanged)
  }

  override suspend fun close() = worker.terminate()

  override suspend fun newTransaction(): AsyncTransacter.Transaction {
    val enclosing = transaction
    val transaction = Transaction(enclosing)
    this.transaction = transaction
    if (enclosing == null) {
      worker.run("BEGIN TRANSACTION")
    }

    return transaction
  }

  override fun currentTransaction(): AsyncTransacter.Transaction? = transaction

  private inner class Transaction(
    override val enclosingTransaction: AsyncTransacter.Transaction?
  ) : AsyncTransacter.Transaction() {
    override suspend fun endTransaction(successful: Boolean) {
      if (enclosingTransaction == null) {
        if (successful) {
          worker.run("END TRANSACTION")
        } else {
          worker.run("ROLLBACK TRANSACTION")
        }
      }
      transaction = enclosingTransaction
    }
  }

  private suspend fun Worker.sendMessage(id: Int, message: dynamic): WorkerData = suspendCancellableCoroutine { continuation ->
    val messageListener = object : EventListener {
      override fun handleEvent(event: Event) {
        val data = event.unsafeCast<MessageEvent>().data.unsafeCast<WorkerData>()
        if (data.id == id) {
          removeEventListener("message", this)
          if (data.error != null) {
            continuation.resumeWithException(JsWorkerException(data.error!!))
          } else {
            continuation.resume(data)
          }
        }
      }
    }

    val errorListener = object : EventListener {
      override fun handleEvent(event: Event) {
        removeEventListener("error", this)
        continuation.resumeWithException(JsWorkerException(event.toString()))
      }
    }

    addEventListener("message", messageListener)
    addEventListener("error", errorListener)

    postMessage(message)

    continuation.invokeOnCancellation {
      removeEventListener("message", messageListener)
      removeEventListener("error", errorListener)
    }
  }

  private suspend fun Worker.run(sql: String) {
    val messageId = messageCounter++
    val message = jsObject<WorkerMessage> {
      this.id = messageId
      this.action = "exec"
      this.sql = sql
    }

    sendMessage(messageId, message)
  }
}

class JsWorkerSqlCursor(private val table: QueryResults) : AsyncSqlCursor {
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
