package app.cash.sqldelight.db

sealed interface QueryResult<T> {
  val value: T get() = throw IllegalStateException(
    """
      The driver used with SQLDelight is asynchronous, so SQLDelight should be configured for
      asynchronous usage:

      sqldelight {
        MyDatabase {
          generateAsync = true
        }
      }
    """.trimIndent(),
  )

  suspend fun await(): T

  data class Value<T>(override val value: T) : QueryResult<T> {
    override suspend fun await() = value
  }

  class AsyncValue<T>(private inline val getter: suspend () -> T) : QueryResult<T> {
    override suspend fun await() = getter()
  }

  /**
   * A [QueryResult] representation of a Kotlin [Unit].
   *
   * Equivalent to `QueryResult.Value(Unit)`.
   */
  object Unit : QueryResult<kotlin.Unit> {
    override val value: kotlin.Unit = kotlin.Unit

    override suspend fun await() {
    }
  }
}
