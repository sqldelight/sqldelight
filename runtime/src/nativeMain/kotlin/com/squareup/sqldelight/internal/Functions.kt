package com.squareup.sqldelight.internal

import co.touchlab.stately.collections.frozenCopyOnWriteList
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import com.squareup.sqldelight.Query
import kotlin.native.concurrent.Worker

actual fun copyOnWriteList(): MutableList<Query<*>> {
  return frozenCopyOnWriteList()
}

internal actual fun <T> copyOnWriteList(): MutableList<T> {
  return frozenCopyOnWriteList()
}

internal actual class QueryLock {
  internal val lock = Lock()
}

internal actual inline fun <T> QueryLock.withLock(block: () -> T) = lock.withLock(block)

internal actual fun currentThreadId(): Long = Worker.current.id.toLong()
