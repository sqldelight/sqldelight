package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query

internal typealias Supplier<T> = () -> T

expect fun copyOnWriteList(): MutableList<Query<*>>

internal expect fun <T> threadLocalRef(value: T): Supplier<T>

internal expect class QueryLock()

internal expect inline fun <T> QueryLock.withLock(block: () -> T): T

internal expect fun <T> sharedSet(): MutableSet<T>

internal expect fun <T, R> sharedMap(): MutableMap<T, R>