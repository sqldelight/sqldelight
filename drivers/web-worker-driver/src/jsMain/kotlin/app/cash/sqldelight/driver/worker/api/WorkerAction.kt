package app.cash.sqldelight.driver.worker.api

internal actual sealed interface WorkerAction

@Suppress("NOTHING_TO_INLINE", "FunctionName")
internal actual inline fun WorkerAction(value: String) = value.unsafeCast<WorkerAction>()
