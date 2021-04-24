package com.squareup.sqldelight.logs

expect class LinkedList<T>(objectPoolSize: Int = 0) {
  fun add(element: T): Boolean
  fun clear(): Unit
  operator fun get(index: Int): T
}
