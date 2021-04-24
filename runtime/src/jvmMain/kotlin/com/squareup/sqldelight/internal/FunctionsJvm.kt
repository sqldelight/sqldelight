package com.squareup.sqldelight.internal

import java.util.concurrent.CopyOnWriteArrayList

actual fun <T> copyOnWriteList(): MutableList<T> {
  return CopyOnWriteArrayList()
}

internal actual fun <T> threadLocalRef(value: T): () -> T {
  val threadLocal = ThreadLocal<T>()
  threadLocal.set(value)
  return { threadLocal.get() }
}

internal actual fun <T> sharedSet(): MutableSet<T> = mutableSetOf()

internal actual fun <T, R> sharedMap(): MutableMap<T, R> = mutableMapOf()
