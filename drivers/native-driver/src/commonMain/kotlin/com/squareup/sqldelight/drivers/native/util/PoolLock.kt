package com.squareup.sqldelight.drivers.native.util

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class PoolLock() {
  fun <R> withLock(
    action: CriticalSection.() -> R
  ): R

  fun signalAvailability()
  fun close(): Boolean

  inner class CriticalSection {
    /**
     * Evaluate the given block. If the block produces a non-null result, return it immediately. Otherwise, suspend the
     * calling thread for an eventual resumption by a [PoolLock.signalAvailability] call.
     */
    fun <R> loopUntilAvailableResult(block: () -> R?): R
  }
}
