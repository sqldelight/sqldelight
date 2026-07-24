package app.cash.sqldelight.driver.worker.api

/**
 * Messages sent by the SQLDelight driver to the worker.
 */
internal external interface JsWorkerRequest {
  /**
   * A unique identifier used to identify responses to this message
   * @see WorkerResponse.id
   */
  var id: Int

  /**
   * The action that the worker should run.
   * @see WorkerAction
   */
  var action: WorkerAction

  /**
   * The SQL to execute
   */
  var sql: String?

  /**
   * SQL parameters to bind to the given [sql]
   */
  var params: Array<Any?>?

  /**
   * Database name used to configure a persistent worker.
   */
  var databaseName: String?
}

internal fun buildRequest(builder: JsWorkerRequest.() -> Unit): JsWorkerRequest {
  val request = js("{}").unsafeCast<JsWorkerRequest>()
  builder(request)
  return request
}
