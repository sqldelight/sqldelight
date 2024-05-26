package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.worker.api.WorkerAction
import app.cash.sqldelight.driver.worker.api.WorkerActions
import app.cash.sqldelight.driver.worker.api.WorkerResultWithRowCount
import app.cash.sqldelight.driver.worker.api.WorkerWrapperRequest
import app.cash.sqldelight.driver.worker.expected.JsWorkerSqlCursor
import app.cash.sqldelight.driver.worker.expected.JsWorkerSqlPreparedStatement
import app.cash.sqldelight.driver.worker.expected.Worker
import app.cash.sqldelight.driver.worker.expected.checkWorkerResults

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
  private val wrapper = WorkerWrapper(worker)

  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> QueryResult<R>,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<R> {
    val bound = JsWorkerSqlPreparedStatement()
    binders?.invoke(bound)

    return QueryResult.AsyncValue {
      val response = wrapper.sendMessage(
        action = WorkerActions.exec,
        sql = sql,
        statement = bound,
      )

      return@AsyncValue mapper(JsWorkerSqlCursor(checkWorkerResults(response.result))).await()
    }
  }

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<Long> {
    val bound = JsWorkerSqlPreparedStatement()
    binders?.invoke(bound)

    return QueryResult.AsyncValue {
      val response = wrapper.sendMessage(
        action = WorkerActions.exec,
        sql = sql,
        statement = bound,
      )
      checkWorkerResults(response.result)
      return@AsyncValue response.rowCount
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

  override fun close() = wrapper.terminate()

  override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.AsyncValue {
    val enclosing = transaction
    val transaction = Transaction(enclosing)
    this.transaction = transaction
    if (enclosing == null) {
      wrapper.sendMessage(WorkerActions.beginTransaction)
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
          wrapper.sendMessage(WorkerActions.endTransaction)
        } else {
          wrapper.sendMessage(WorkerActions.rollbackTransaction)
        }
      }
      transaction = enclosingTransaction
    }
  }

  private suspend fun WorkerWrapper.sendMessage(
    action: WorkerAction,
    sql: String? = null,
    statement: JsWorkerSqlPreparedStatement? = null,
  ): WorkerResultWithRowCount {
    val id = messageCounter++

    println("beforeExecute")

    return execute(
      WorkerWrapperRequest(
        id = id,
        action = action,
        sql = sql,
        statement = statement,
      ),
    )
  }

}
