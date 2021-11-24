package com.squareup.sqldelight.internal

actual fun <T> copyOnWriteList(): MutableList<T> {
  return mutableListOf()
}

internal actual fun currentThreadId(): Long = 0
