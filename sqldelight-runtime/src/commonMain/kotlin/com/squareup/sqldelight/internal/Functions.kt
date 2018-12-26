package com.squareup.sqldelight.internal

/**
 * Shared set. Not concurrent on jvm and js
 */
expect fun <T> sharedSet():MutableSet<T>