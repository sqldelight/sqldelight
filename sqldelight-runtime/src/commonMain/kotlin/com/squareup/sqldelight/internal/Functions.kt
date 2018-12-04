package com.squareup.sqldelight.internal

/**
 * Delegates to regular lazy on jvm and js, and atomicLazy on native
 */
expect fun <T> safeLazy(initializer: () -> T): Lazy<T>

/**
 * Shared set. Not concurrent on jvm and js
 */
expect fun <T> sharedSet():MutableSet<T>