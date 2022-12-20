package app.cash.sqldelight.driver.worker.api

/**
 * Data returned by the worker after posting a message.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
internal external interface WorkerResponse {
  /**
   * An error returned by the worker, could be undefined.
   */
  var error: String?

  /**
   * The id of the message that this data is in response to. Matches the value that was posted in [WorkerRequest.id].
   * @see WorkerRequest.id
   */
  var id: Int

  /**
   * An array of [WorkerResult] containing any of the rows that were returned by the query.
   * The array could be empty! In general, we only expect one QueryResult to be returned.
   */
  var result: WorkerResult?
}
