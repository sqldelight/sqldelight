package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query

actual fun copyOnWriteList(): MutableList<Query<*>> {
  return mutableListOf()
}