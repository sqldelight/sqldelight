package com.squareup.sqldelight.internal

actual open class Atomic<V> actual constructor(private var value: V) {
  actual fun get() = value
  actual fun set(value: V) { this.value = value }
}

actual class AtomicBoolean actual constructor(value: Boolean) : Atomic<Boolean>(value)