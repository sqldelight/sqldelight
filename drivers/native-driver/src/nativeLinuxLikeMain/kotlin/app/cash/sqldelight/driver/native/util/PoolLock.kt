package app.cash.sqldelight.driver.native.util

import co.touchlab.stately.concurrency.AtomicBoolean
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_signal
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock

internal actual class PoolLock actual constructor() {
  private val isActive = AtomicBoolean(true)
  private val mutex = nativeHeap.alloc<pthread_mutex_t>()
    .apply { pthread_mutex_init(ptr, null) }
  private val cond = nativeHeap.alloc<pthread_cond_t>()
    .apply { pthread_cond_init(ptr, null) }

  actual fun <R> withLock(
    action: CriticalSection.() -> R
  ): R {
    check(isActive.value)
    pthread_mutex_lock(mutex.ptr)

    val result: R

    try {
      result = action(CriticalSection())
    } finally {
      pthread_mutex_unlock(mutex.ptr)
    }

    return result
  }

  actual fun notifyConditionChanged() {
    pthread_cond_signal(cond.ptr)
  }

  actual fun close(): Boolean {
    if (isActive.compareAndSet(expected = true, new = false)) {
      pthread_cond_destroy(cond.ptr)
      pthread_mutex_destroy(mutex.ptr)
      nativeHeap.free(cond)
      nativeHeap.free(mutex)
      return true
    }

    return false
  }

  actual inner class CriticalSection {
    actual fun <R> loopForConditionalResult(block: () -> R?): R {
      check(isActive.value)

      var result = block()

      while (result == null) {
        pthread_cond_wait(cond.ptr, mutex.ptr)
        result = block()
      }

      return result
    }

    actual fun loopUntilConditionalResult(block: () -> Boolean) {
      check(isActive.value)

      while (!block()) {
        pthread_cond_wait(cond.ptr, mutex.ptr)
      }
    }
  }
}
