package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.driver.worker.api.WorkerResult
import app.cash.sqldelight.driver.worker.util.isArray

internal actual fun checkWorkerResults(results: WorkerResult?): WorkerResult {
  checkNotNull(results) { "The worker result was null " }
  val values = results.values
  check(values != null && isArray(values)) { "The worker result values were not an array" }
  return results
}
