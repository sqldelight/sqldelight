package com.squareup.sqldelight.logs

import co.touchlab.stately.collections.SharedLinkedList

actual class LinkedList<T> actual constructor(objectPoolSize: Int) {
    private val list = SharedLinkedList<T>(objectPoolSize)
    actual fun add(element: T): Boolean = list.add(element)
    actual fun clear() { list.clear() }
    actual operator fun get(index: Int): T = list[index]
}
