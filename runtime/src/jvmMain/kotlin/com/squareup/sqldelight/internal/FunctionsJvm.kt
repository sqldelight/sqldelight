package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query
import java.util.concurrent.CopyOnWriteArrayList

actual fun copyOnWriteList(): MutableList<Query<*>> {
  return CopyOnWriteArrayList()
}

@JvmName("copyOnWriteListGeneric")
internal actual fun <T> copyOnWriteList(): MutableList<T> {
  return CopyOnWriteArrayList()
}

internal actual class QueryLock

internal actual inline fun <T> QueryLock.withLock(block: () -> T): T {
  synchronized(this) {
    return block()
  }
}
