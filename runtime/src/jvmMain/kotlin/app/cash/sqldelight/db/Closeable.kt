package app.cash.sqldelight.db

import kotlin.io.use as kotlinIoUse

actual typealias Closeable = java.io.Closeable

actual inline fun <T : Closeable?, R> T.use(body: (T) -> R): R = kotlinIoUse(body)
