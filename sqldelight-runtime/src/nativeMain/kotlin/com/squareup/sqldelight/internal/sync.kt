package com.squareup.sqldelight.internal

internal actual inline fun <R> sync(lock: Any, body: () -> R): R = body()
