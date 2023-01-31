package app.cash.sqldelight.driver.worker.api

/**
 * Data returned by the worker after posting a message.
 */
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
   * A [WorkerResult] containing any values that were returned by the worker.
   * @see WorkerResult
   */
  var results: WorkerResult
}
