package app.cash.sqldelight.driver.worker.api

internal sealed interface WorkerAction {
  companion object {
    /**
     * Execute a SQL statement.
     */
    inline val exec: WorkerAction get() = WorkerAction("exec")

    /**
     * Begin a transaction in the underlying SQL implementation.
     */
    inline val beginTransaction: WorkerAction get() = WorkerAction("begin_transaction")

    /**
     * End or commit a transaction in the underlying SQL implementation.
     */
    inline val endTransaction: WorkerAction get() = WorkerAction("end_transaction")

    /**
     * Roll back a transaction in the underlying SQL implementation.
     */
    inline val rollbackTransaction: WorkerAction get() = WorkerAction("rollback_transaction")
  }
}

@Suppress("NOTHING_TO_INLINE", "FunctionName")
/**
 * @suppress
 */
internal inline fun WorkerAction(value: String) = value.unsafeCast<WorkerAction>()
