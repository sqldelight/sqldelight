package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.driver.worker.api.WorkerResultWithRowCount
import app.cash.sqldelight.driver.worker.api.WorkerWrapperRequest
import app.cash.sqldelight.driver.worker.expected.Worker

internal expect class WorkerWrapper(worker: Worker) {
  suspend fun execute(
    request: WorkerWrapperRequest,
  ): WorkerResultWithRowCount

  fun terminate()
}
