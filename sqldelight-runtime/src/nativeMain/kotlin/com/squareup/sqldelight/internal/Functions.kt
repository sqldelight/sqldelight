package com.squareup.sqldelight.internal

import co.touchlab.stately.collections.frozenCopyOnWriteList
import com.squareup.sqldelight.Query

actual fun copyOnWriteList(): MutableList<Query<*>> {
  return frozenCopyOnWriteList()
}