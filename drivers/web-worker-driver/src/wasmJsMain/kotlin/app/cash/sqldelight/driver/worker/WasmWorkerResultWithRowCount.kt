package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.driver.worker.api.WasmWorkerResponse
import app.cash.sqldelight.driver.worker.api.WorkerResultWithRowCount

internal class WasmWorkerResultWithRowCount(
  private val data: WasmWorkerResponse,
) : WorkerResultWithRowCount {
  override val rowCount: Long
    get() = when {
      data.results.values?.length == 0 -> 0L
      else -> data.results.values?.get(0)?.get(0)?.unsafeCast<JsNumber>()?.toDouble()
        ?.toLong() ?: 0L
    }

  override val result = data.results
}
