package com.squareup.sqldelight.drivers.native.util

import co.touchlab.stately.concurrency.AtomicBoolean

internal class PoolLock() {
  private val isActive = AtomicBoolean(true)
  private val mutex = createPthreadMutex()
  private val cond = createPthreadCondition()

  fun <R> withLock(
    action: CriticalSection.() -> R
  ): R {
    check(isActive.value)
    mutex.lock()

    val result: R

    try {
      result = action(CriticalSection())
    } finally {
      mutex.unlock()
    }

    return result
  }

  fun close(): Boolean {
    if (isActive.compareAndSet(expected = true, new = false)) {
      cond.destroy()
      mutex.destroy()
      return true
    }

    return false
  }

  inner class CriticalSection {
    /**
     * Evaluate the given block. If the block produces a non-null result, return it immediately. Otherwise, suspend the
     * calling thread for an eventual resumption by a [PoolLock.signalAvailability] call.
     */
    fun <R> loopUntilAvailableResult(block: () -> R?): R {
      check(isActive.value)

      var result = block()

      while (result == null) {
        cond.wait(mutex)
        result = block()
      }

      return result
    }

    fun signalAvailability() {
      // Signalling is deliberately made to require lock acquisition, so that it is serialized together with block
      // invocations in [loopUntilAvailableResult].
      cond.signal()
    }
  }
}
