package com.squareup.sqldelight.internal

actual fun <T> copyOnWriteList(): MutableList<T> {
  return mutableListOf()
}
