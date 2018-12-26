package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query
import java.util.concurrent.CopyOnWriteArrayList

actual fun copyOnWriteList(): MutableList<Query<*>> {
  return CopyOnWriteArrayList()
}