package com.squareup.sqldelight.internal

actual fun <T> sharedSet(): MutableSet<T> = mutableSetOf<T>()