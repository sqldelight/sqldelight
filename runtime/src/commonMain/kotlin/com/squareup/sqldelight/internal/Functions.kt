package com.squareup.sqldelight.internal

expect fun <T> copyOnWriteList(): MutableList<T>

internal expect fun currentThreadId(): Long
