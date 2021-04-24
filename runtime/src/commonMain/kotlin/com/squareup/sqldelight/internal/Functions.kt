package com.squareup.sqldelight.internal

internal typealias Supplier<T> = () -> T

expect fun <T> copyOnWriteList(): MutableList<T>

internal expect fun <T> threadLocalRef(value: T): Supplier<T>

internal expect fun <T> sharedSet(): MutableSet<T>

internal expect fun <T, R> sharedMap(): MutableMap<T, R>
