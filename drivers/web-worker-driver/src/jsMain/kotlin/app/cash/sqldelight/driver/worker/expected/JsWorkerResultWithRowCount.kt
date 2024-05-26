package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.driver.worker.api.JsWorkerResponse
import app.cash.sqldelight.driver.worker.api.WorkerResult
import app.cash.sqldelight.driver.worker.api.WorkerResultWithRowCount

internal class JsWorkerResultWithRowCount(
  private val data: JsWorkerResponse,
) :
  WorkerResultWithRowCount {
  override val rowCount: Long by lazy {
    when {
      data.results.values.isEmpty() -> 0L
      else -> data.results.values[0][0].unsafeCast<Double>().toLong()
    }
  }

  override val result: WorkerResult = data.results
}
