package app.cash.sqldelight.internal

import co.touchlab.stately.concurrency.value
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

actual class Atomic<V> actual constructor(value: V) {
  private val atomicRef = AtomicReference(value)

  actual fun get() = atomicRef.value
  actual fun set(value: V) {
    atomicRef.value = value.freeze()
  }
}

actual class AtomicBoolean actual constructor(value: Boolean) {
  private val atomicBoolean = co.touchlab.stately.concurrency.AtomicBoolean(value)

  actual fun get() = atomicBoolean.value
  actual fun set(value: Boolean) {
    atomicBoolean.value = value
  }
}
