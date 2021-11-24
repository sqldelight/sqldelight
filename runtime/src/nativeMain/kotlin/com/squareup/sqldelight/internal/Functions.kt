package com.squareup.sqldelight.internal

import co.touchlab.stately.collections.frozenCopyOnWriteList
import kotlin.native.concurrent.Worker

actual fun <T> copyOnWriteList(): MutableList<T> {
  return frozenCopyOnWriteList()
}

internal actual fun currentThreadId(): Long = Worker.current.id.toLong()
