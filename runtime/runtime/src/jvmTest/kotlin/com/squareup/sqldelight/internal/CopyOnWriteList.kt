package com.squareup.sqldelight.internal

import java.util.concurrent.CopyOnWriteArrayList

actual fun <T> copyOnWriteList(): MutableList<T> {
  return CopyOnWriteArrayList()
}
