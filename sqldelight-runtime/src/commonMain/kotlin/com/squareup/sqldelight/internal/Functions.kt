package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query
import kotlin.reflect.KProperty

expect fun copyOnWriteList(): MutableList<Query<*>>

internal expect fun <T> threadLocalRef(value: T): () -> T


internal expect class QueryLock()

internal expect inline fun <T> QueryLock.withLock(block: () -> T): T

internal expect fun <T> sharedSet(): MutableSet<T>