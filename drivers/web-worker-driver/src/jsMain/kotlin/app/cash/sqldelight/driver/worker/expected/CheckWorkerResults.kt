package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.driver.worker.api.WorkerResult

internal actual fun checkWorkerResults(results: WorkerResult?): WorkerResult {
  checkNotNull(results) { "The worker result was null " }
  check(js("Array.isArray(results.values)").unsafeCast<Boolean>()) { "The worker result values were not an array" }
  return results
}
