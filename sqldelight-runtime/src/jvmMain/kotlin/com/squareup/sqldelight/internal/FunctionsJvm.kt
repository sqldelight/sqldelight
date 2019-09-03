package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query
import java.util.concurrent.CopyOnWriteArrayList

actual fun copyOnWriteList(): MutableList<Query<*>> {
  return CopyOnWriteArrayList()
}

internal actual class QueryLock

internal actual inline fun <T> QueryLock.withLock(block: () -> T): T {
  synchronized(this) {
    return block()
  }
}

internal actual fun <T> threadLocalRef(value: T): () -> T {
  val threadLocal = ThreadLocal<T>()
  threadLocal.set(value)
  return { threadLocal.get() }
}

internal actual fun <T> sharedSet(): MutableSet<T> = mutableSetOf()

internal actual fun <T, R> sharedMap(): MutableMap<T, R> = mutableMapOf()