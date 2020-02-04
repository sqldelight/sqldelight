package com.squareup.sqldelight.logs

actual class LinkedList<T> actual constructor(objectPoolSize: Int) {
    private val list = mutableListOf<T>()
    actual fun add(element: T): Boolean = list.add(element)
    actual fun clear() = list.clear()
    actual operator fun get(index: Int): T = list[index]
}
