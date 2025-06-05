package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.driver.worker.api.WorkerResult

internal expect fun checkWorkerResults(results: WorkerResult?): WorkerResult
