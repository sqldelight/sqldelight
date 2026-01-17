package app.cash.sqldelight.driver.worker.api

internal actual sealed interface WorkerAction

@Suppress("NOTHING_TO_INLINE", "FunctionName", "RedundantSuppression")
internal actual inline fun WorkerAction(value: String) = value.unsafeCast<WorkerAction>()
