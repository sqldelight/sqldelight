package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query
import kotlin.reflect.KProperty

actual fun copyOnWriteList(): MutableList<Query<*>> {
  return mutableListOf()
}

internal actual class QueryLock

internal actual inline fun <T> QueryLock.withLock(block: () -> T): T {
  return block()
}

internal actual fun <T> threadLocalRef(value: T): () -> T {
  return { value }
}

internal actual fun <T> sharedSet(): MutableSet<T> = mutableSetOf()