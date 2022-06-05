package app.cash.sqldelight.async.coroutines

import app.cash.sqldelight.Query

suspend fun <T : Any> Query<T>.awaitAsList(): List<T> = execute { cursor ->
  val result = mutableListOf<T>()
  while (cursor.next()) result.add(mapper(cursor))
  result
}.await()

suspend fun <T : Any> Query<T>.awaitAsOne(): T {
  return awaitAsOneOrNull()
    ?: throw NullPointerException("ResultSet returned null for $this")
}

suspend fun <T : Any> Query<T>.awaitAsOneOrNull(): T? = execute { cursor ->
  if (!cursor.next()) return@execute null
  val value = mapper(cursor)
  check(!cursor.next()) { "ResultSet returned more than 1 row for $this" }
  value
}.await()
