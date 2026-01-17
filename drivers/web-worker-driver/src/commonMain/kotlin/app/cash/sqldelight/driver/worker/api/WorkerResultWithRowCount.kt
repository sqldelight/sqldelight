package app.cash.sqldelight.driver.worker.api

internal interface WorkerResultWithRowCount {
  val result: WorkerResult
  val rowCount: Long
}
