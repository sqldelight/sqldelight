package com.squareup.sqldelight.drivers.native

internal interface Borrowed<T> {
  val value: T
  fun release()
}
