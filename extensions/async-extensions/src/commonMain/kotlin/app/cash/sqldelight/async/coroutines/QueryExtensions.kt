package app.cash.sqldelight.async.coroutines

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.StaleWindowException

suspend fun <T : Any> ExecutableQuery<T>.awaitAsList(): List<T> = execute { cursor ->
  val first = cursor.next()
  val result = mutableListOf<T>()

  // If the cursor isn't async, we want to preserve the blocking semantics and execute it synchronously
  when (first) {
    is QueryResult.AsyncValue -> {
      QueryResult.AsyncValue {
        if (first.await()) {
          try {
            result.add(mapper(cursor))
          } catch (_: StaleWindowException) {
            // Row became stale due to concurrent modification. Skip and continue.
          }
        } else return@AsyncValue result
        while (cursor.next().await()) {
          try {
            result.add(mapper(cursor))
          } catch (_: StaleWindowException) {
            // Row became stale due to concurrent modification. Skip and continue.
            continue
          }
        }
        result
      }
    }

    is QueryResult.Value -> {
      if (first.value) {
        try {
          result.add(mapper(cursor))
        } catch (_: StaleWindowException) {
          // Row became stale due to concurrent modification. Skip and continue.
        }
      } else return@execute QueryResult.Value(result)
      while (cursor.next().value) {
        try {
          result.add(mapper(cursor))
        } catch (_: StaleWindowException) {
          // Row became stale due to concurrent modification. Skip and continue.
          continue
        }
      }
      QueryResult.Value(result)
    }
  }
}.await()

suspend fun <T : Any> ExecutableQuery<T>.awaitAsOne(): T {
  return awaitAsOneOrNull()
    ?: throw NullPointerException("ResultSet returned null for $this")
}

suspend fun <T : Any> ExecutableQuery<T>.awaitAsOneOrNull(): T? = execute { cursor ->
  val next = cursor.next()

  // If the cursor isn't async, we want to preserve the blocking semantics and execute it synchronously
  when (next) {
    is QueryResult.AsyncValue -> {
      QueryResult.AsyncValue {
        if (!next.await()) return@AsyncValue null
        val value = mapper(cursor)
        check(!cursor.next().await()) { "ResultSet returned more than 1 row for $this" }
        value
      }
    }

    is QueryResult.Value -> {
      if (!next.value) return@execute QueryResult.Value(null)
      val value = try {
        mapper(cursor)
      } catch (e: StaleWindowException) {
        // Row became stale due to concurrent modification. Treat as if no row exists.
        return@execute QueryResult.Value(null)
      }
      check(!cursor.next().value) { "ResultSet returned more than 1 row for $this" }
      QueryResult.Value(value)
    }
  }
}.await()
