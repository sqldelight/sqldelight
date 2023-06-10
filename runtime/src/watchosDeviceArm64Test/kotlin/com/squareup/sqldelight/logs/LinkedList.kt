package com.squareup.sqldelight.logs

// https://github.com/cashapp/sqldelight/issues/4257
// this code isn't used but need for compiling
actual class LinkedList<T>
actual constructor(objectPoolSize: Int) {
  private val list = mutableListOf<T>()
  actual fun add(element: T) = list.add(element)

  actual fun clear() {
    list.clear()
  }

  actual operator fun get(index: Int) = list[index]
}
