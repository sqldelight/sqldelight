package app.cash.sqldelight.driver.worker.expected

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.worker.api.WorkerResult

internal expect class JsWorkerSqlCursor(result: WorkerResult) : SqlCursor
