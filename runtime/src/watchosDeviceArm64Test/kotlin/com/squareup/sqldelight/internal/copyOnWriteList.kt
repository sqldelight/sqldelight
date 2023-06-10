package com.squareup.sqldelight.internal

// https://github.com/cashapp/sqldelight/issues/4257
// this code isn't used but need for compiling
actual fun <T> copyOnWriteList(): MutableList<T> = mutableListOf()