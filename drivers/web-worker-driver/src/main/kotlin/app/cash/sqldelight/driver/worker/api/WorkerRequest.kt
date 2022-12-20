package app.cash.sqldelight.driver.worker.api

/**
 * Messages sent by the SQLDelight driver to the worker.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
external interface WorkerRequest {
  /**
   * A unique identifier used to identify responses to this message
   * @see WorkerResponse.id
   */
  var id: Int

  /**
   * The action that the worker should run. Can be one of the following values:
   *
   * - `exec`: Execute the given [sql] with the given [params]
   * - `begin_transaction`: Begin a transaction
   * - `end_transaction`: End/commit a transaction
   * - `rollback_transaction`: Rollback the current transaction
   */
  var action: String

  /**
   * The SQL to execute
   */
  var sql: String?

  /**
   * SQL parameters to bind to the given [sql]
   */
  var params: Array<Any?>?
}
