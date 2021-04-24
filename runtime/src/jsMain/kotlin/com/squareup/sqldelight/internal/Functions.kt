package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query

actual fun copyOnWriteList(): MutableList<Query<*>> {
  return mutableListOf()
}

internal actual fun <T> copyOnWriteList(): MutableList<T> {
  return mutableListOf()
}

internal actual class QueryLock

internal actual inline fun <T> QueryLock.withLock(block: () -> T): T {
  return block()
}
