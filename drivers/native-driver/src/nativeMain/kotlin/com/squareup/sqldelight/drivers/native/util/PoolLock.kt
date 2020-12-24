package com.squareup.sqldelight.drivers.native.util

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

internal class PoolLock {
  private val isActive = AtomicBoolean(true)
  private val mutex = nativeHeap.alloc<pthread_mutex_t>()
  private val cond = nativeHeap.alloc<pthread_cond_t>()

  init {
    pthread_mutex_init(mutex.ptr, null)
    pthread_cond_init(cond.ptr, null)
  }

  fun <R> withLock(
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

  fun close(): Boolean {
    if (isActive.compareAndSet(expected = true, new = false)) {
      pthread_cond_destroy(cond.ptr)
      pthread_mutex_destroy(mutex.ptr)
      nativeHeap.free(cond)
      nativeHeap.free(mutex)
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
        pthread_cond_wait(cond.ptr, mutex.ptr)
        result = block()
      }

      return result
    }

    fun signalAvailability() {
      // Signalling is deliberately made to require lock acquisition, so that it is serialized together with block
      // invocations in [loopUntilAvailableResult].
      pthread_cond_signal(cond.ptr)
    }
  }
}
