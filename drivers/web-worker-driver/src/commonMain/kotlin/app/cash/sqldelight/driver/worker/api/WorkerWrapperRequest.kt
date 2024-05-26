package app.cash.sqldelight.driver.worker.api

import app.cash.sqldelight.driver.worker.expected.JsWorkerSqlPreparedStatement

/**
 * Messages sent by the SQLDelight driver to the worker.
 */
internal data class WorkerWrapperRequest(
  /**
   * A unique identifier used to identify responses to this message
   * @see WorkerResponse.id
   */
  val id: Int,
  /**
   * The action that the worker should run.
   * @see WorkerAction
   */
  val action: WorkerAction,
  /**
   * The SQL to execute
   */
  var sql: String?,

  /**
   * SQL parameters to bind to the given [sql]
   */
  var statement: JsWorkerSqlPreparedStatement?,
)
