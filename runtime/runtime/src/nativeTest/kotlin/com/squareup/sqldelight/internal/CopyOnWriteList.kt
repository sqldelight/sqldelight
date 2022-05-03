package com.squareup.sqldelight.internal

import co.touchlab.stately.collections.frozenCopyOnWriteList

actual fun <T> copyOnWriteList(): MutableList<T> {
  return frozenCopyOnWriteList()
}
