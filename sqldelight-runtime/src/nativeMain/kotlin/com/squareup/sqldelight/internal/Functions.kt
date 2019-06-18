package com.squareup.sqldelight.internal

import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.collections.SharedSet
import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.collections.frozenCopyOnWriteList
import co.touchlab.stately.concurrency.value
import co.touchlab.stately.concurrency.withLock
import com.squareup.sqldelight.Query
import co.touchlab.stately.collections.SharedHashMap

actual fun copyOnWriteList(): MutableList<Query<*>> {
  return frozenCopyOnWriteList()
}

internal actual class QueryLock {
  internal val lock = Lock()
}

internal actual inline fun <T> QueryLock.withLock(block: () -> T) = lock.withLock(block)

internal actual fun <T> threadLocalRef(value: T): () -> T {
  val threadLocal = ThreadLocalRef<T>()
  threadLocal.value = value
  return { threadLocal.value!! }
}

internal actual fun <T> sharedSet(): MutableSet<T> = SharedSet<T>()

internal actual fun <T, R> sharedMap(): MutableMap<T, R> = SharedHashMap<T, R>()