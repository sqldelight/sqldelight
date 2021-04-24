package com.squareup.sqldelight.internal

actual fun <T> copyOnWriteList(): MutableList<T> {
  return mutableListOf()
}

internal actual fun <T> threadLocalRef(value: T): () -> T {
  return { value }
}

internal actual fun <T> sharedSet(): MutableSet<T> = mutableSetOf()

internal actual fun <T, R> sharedMap(): MutableMap<T, R> = mutableMapOf()
