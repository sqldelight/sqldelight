package com.squareup.sqldelight.db

expect interface Closeable {
  fun close()
}

expect inline fun <T : Closeable?, R> T.use(body: (T) -> R): R
