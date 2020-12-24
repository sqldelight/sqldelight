@file:Suppress("UNCHECKED_CAST")

package com.squareup.sqldelight.drivers.native.util

import kotlinx.cinterop.CPrimitiveVar
import kotlinx.cinterop.LongVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_signal
import platform.posix.pthread_cond_wait
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_unlock

actual typealias PthreadMutexPointed = CPrimitiveVar
actual typealias PthreadConditionPointed = CPrimitiveVar

internal actual fun createPthreadMutex(): PthreadMutexPointed {
  val pointer = nativeHeap.alloc<pthread_mutex_tVar>()
  pthread_mutex_init(pointer.ptr, null)
  return pointer
}

internal actual fun createPthreadCondition(): PthreadConditionPointed {
  val pointer = nativeHeap.alloc<pthread_cond_tVar>()
  pthread_cond_init(pointer.ptr, null)
  return pointer
}

internal actual fun PthreadMutexPointed.lock() {
  pthread_mutex_lock((this as LongVarOf<Long>).ptr)
}

internal actual fun PthreadMutexPointed.unlock() {
  pthread_mutex_unlock((this as LongVarOf<Long>).ptr)
}

internal actual fun PthreadConditionPointed.wait(mutex: PthreadMutexPointed) {
  pthread_cond_wait((this as LongVarOf<Long>).ptr, (mutex as LongVarOf<Long>).ptr)
}
internal actual fun PthreadConditionPointed.signal() {
  pthread_cond_signal((this as LongVarOf<Long>).ptr)
}

internal actual fun PthreadMutexPointed.destroy() {
  pthread_mutex_destroy((this as LongVarOf<Long>).ptr)
  nativeHeap.free(this)
}

internal actual fun PthreadConditionPointed.destroy() {
  pthread_cond_destroy((this as LongVarOf<Long>).ptr)
  nativeHeap.free(this)
}
