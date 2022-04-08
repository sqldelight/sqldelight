package app.cash.sqldelight.driver.native.util

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class PoolLock() {
  fun <R> withLock(
    action: CriticalSection.() -> R
  ): R

  /**
   * Select one blocked thread in [CriticalSection.loopForConditionalResult] to be woken up for
   * re-evaluation, if any.
   */
  fun notifyConditionChanged()

  fun close(): Boolean

  inner class CriticalSection {
    /**
     * Evaluate the given lambda of a conditional result in an infinite loop, until the result is
     * available.
     *
     * If null is produced, the current thread enters suspension, and are only woken up for
     * re-evaluation by a subsequent [PoolLock.notifyConditionChanged] call. Note that the lock
     * would not be held by the current thread during its suspension. This allows resources
     * protected by the same lock to remain accessible by other threads, provided that they do not
     * depend on the same conditional result.
     */
    fun <R> loopForConditionalResult(block: () -> R?): R

    fun loopUntilConditionalResult(block: () -> Boolean)
  }
}
