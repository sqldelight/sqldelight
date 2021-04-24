package com.squareup.sqldelight.internal

import java.util.concurrent.CopyOnWriteArrayList

actual fun <T> copyOnWriteList(): MutableList<T> {
  return CopyOnWriteArrayList()
}

internal actual fun currentThreadId(): Long = Thread.currentThread().id
