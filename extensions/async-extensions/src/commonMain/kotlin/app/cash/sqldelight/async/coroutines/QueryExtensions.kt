package app.cash.sqldelight.async.coroutines

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.db.QueryResult

suspend fun <T : Any> ExecutableQuery<T>.awaitAsList(): List<T> = execute { cursor ->
  QueryResult.AsyncValue {
    val result = mutableListOf<T>()
    while (cursor.next().await()) result.add(mapper(cursor))
    result
  }
}.await()

suspend fun <T : Any> ExecutableQuery<T>.awaitAsOne(): T {
  return awaitAsOneOrNull()
    ?: throw NullPointerException("ResultSet returned null for $this")
}

suspend fun <T : Any> ExecutableQuery<T>.awaitAsOneOrNull(): T? = execute { cursor ->
  QueryResult.AsyncValue {
    if (!cursor.next().await()) return@AsyncValue null
    val value = mapper(cursor)
    check(!cursor.next().await()) { "ResultSet returned more than 1 row for $this" }
    value
  }
}.await()
