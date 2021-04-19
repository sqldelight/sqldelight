package com.squareup.sqldelight.drivers.native.util

import kotlin.native.concurrent.AtomicInt

class AtomicBoolean(value_: Boolean) {
  private val atom = AtomicInt(boolToInt(value_))
  var value: Boolean
    get() = atom.value != 0
    set(value) {
      atom.value = boolToInt(value)
    }

  fun compareAndSet(expected: Boolean, new: Boolean): Boolean =
    atom.compareAndSet(boolToInt(expected), boolToInt(new))

  private fun boolToInt(b: Boolean): Int = if (b) {
    1
  } else {
    0
  }
}