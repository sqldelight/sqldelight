package com.squareup.sqldelight.internal

import co.touchlab.stately.collections.SharedHashMap
import co.touchlab.stately.collections.SharedSet
import co.touchlab.stately.collections.frozenCopyOnWriteList
import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value

actual fun <T> copyOnWriteList(): MutableList<T> {
  return frozenCopyOnWriteList()
}

internal actual fun <T> threadLocalRef(value: T): () -> T {
  val threadLocal = ThreadLocalRef<T>()
  threadLocal.value = value
  return { threadLocal.value!! }
}

internal actual fun <T> sharedSet(): MutableSet<T> = SharedSet<T>()

internal actual fun <T, R> sharedMap(): MutableMap<T, R> = SharedHashMap<T, R>()
