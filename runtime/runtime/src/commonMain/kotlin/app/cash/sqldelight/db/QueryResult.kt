package app.cash.sqldelight.db

import kotlin.coroutines.suspendCoroutine

sealed interface QueryResult<T> {
  val value: T get() = throw IllegalStateException("""
      The driver used with SQLDelight is asyncronous, so SQLDelight should be configured for
      asyncronous usage:
      
      sqldelight {
        MyDatabase {
          asyncronous = true
        }
      }
    """.trimIndent())

  suspend fun get(): T

  data class Value<T>(override val value: T): QueryResult<T> {
    override suspend fun get() = value
  }

  class AsyncronousValue<T> : QueryResult<T> {
    fun set(value: T) {
      TODO()
    }

    override suspend fun get() = suspendCoroutine<T> { continuation ->
      TODO()
    }
  }
}
