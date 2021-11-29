package com.squareup.sqldelight.internal

expect fun <T> copyOnWriteList(): MutableList<T>
