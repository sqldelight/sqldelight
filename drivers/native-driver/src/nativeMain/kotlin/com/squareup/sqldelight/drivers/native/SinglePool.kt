package com.squareup.sqldelight.drivers.native

import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock

/**
 * Simple single entry "pool". Sufficient for the vast majority of SQLite needs, but will need a
 * more exotic structure for an actual pool.
 */
internal class SinglePool<T>(producer: () -> T) {
  private val lock = Lock()
  private val borrowed = AtomicBoolean(false)

  internal val entry = producer()

  fun <R> access(block: (T) -> R): R = lock.withLock {
    block(entry)
  }

  fun borrowEntry(): Borrowed {
    lock.lock()
    assert(!borrowed.value)
    borrowed.value = true
    return Borrowed(entry)
  }

  inner class Borrowed(val entry: T) {
    fun release() {
      borrowed.value = false
      lock.unlock()
    }
  }
}
