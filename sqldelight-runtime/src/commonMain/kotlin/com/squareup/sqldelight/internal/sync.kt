package com.squareup.sqldelight.internal

internal expect inline fun <R> sync(lock: Any, body: () -> R): R
