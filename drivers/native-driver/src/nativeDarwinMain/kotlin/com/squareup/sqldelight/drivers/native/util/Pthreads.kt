package com.squareup.sqldelight.drivers.native.util

import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix._opaque_pthread_cond_t
import platform.posix._opaque_pthread_mutex_t
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

actual typealias PthreadMutexPointed = _opaque_pthread_mutex_t
actual typealias PthreadConditionPointed = _opaque_pthread_cond_t

internal actual fun createPthreadMutex(): PthreadMutexPointed {
  val pointer = nativeHeap.alloc<pthread_mutex_t>()
  pthread_mutex_init(pointer.ptr, null)
  return pointer
}

internal actual fun createPthreadCondition(): PthreadConditionPointed {
  val pointer = nativeHeap.alloc<pthread_cond_t>()
  pthread_cond_init(pointer.ptr, null)
  return pointer
}

internal actual fun PthreadMutexPointed.lock() { pthread_mutex_lock(this.ptr) }
internal actual fun PthreadMutexPointed.unlock() { pthread_mutex_unlock(this.ptr) }
internal actual fun PthreadConditionPointed.wait(mutex: PthreadMutexPointed) { pthread_cond_wait(this.ptr, mutex.ptr) }
internal actual fun PthreadConditionPointed.signal() { pthread_cond_signal(this.ptr) }

internal actual fun PthreadMutexPointed.destroy() {
  pthread_mutex_destroy(this.ptr)
  nativeHeap.free(this)
}

internal actual fun PthreadConditionPointed.destroy() {
  pthread_cond_destroy(this.ptr)
  nativeHeap.free(this)
}
