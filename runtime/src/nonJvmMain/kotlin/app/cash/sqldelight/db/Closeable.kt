package app.cash.sqldelight.db

actual interface Closeable {
  actual fun close()
}

actual inline fun <T : Closeable?, R> T.use(body: (T) -> R): R {
  var exception: Throwable? = null
  try {
    return body(this)
  } catch (e: Throwable) {
    exception = e
    throw e
  } finally {
    when {
      this == null -> {}
      exception == null -> close()
      else ->
        try {
          close()
        } catch (closeException: Throwable) {
          // Nothing to do...
        }
    }
  }
}
