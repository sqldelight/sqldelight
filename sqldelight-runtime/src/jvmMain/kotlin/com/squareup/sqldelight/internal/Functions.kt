package com.squareup.sqldelight.internal

actual fun <T> safeLazy(initializer: () -> T): Lazy<T> = lazy(initializer)

actual fun <T> sharedSet(): MutableSet<T> = mutableSetOf<T>()