package app.cash.sqldelight.coroutines

import app.cash.sqldelight.async.AsyncExecutableQuery
import app.cash.sqldelight.async.db.AsyncSqlDriver
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T : Any> AsyncExecutableQuery<T>.awaitAsOne(): T = suspendCoroutine { continuation ->
  executeAsOne({ continuation.resume(it) }) { continuation.resumeWithException(it) }.start()
}

suspend fun <T : Any> AsyncExecutableQuery<T>.awaitAsOneOrNull(): T? = suspendCoroutine { continuation ->
  executeAsOneOrNull({ continuation.resume(it) }) { continuation.resumeWithException(it) }.start()
}

suspend fun <T : Any> AsyncExecutableQuery<T>.awaitAsList(): List<T> = suspendCoroutine { continuation ->
  executeAsList().onSuccess { continuation.resume(it) }.onError { continuation.resumeWithException(it) }.start()
}

suspend fun <T : Any> AsyncSqlDriver.Callback<T>.await(): T = suspendCoroutine { continuation ->
  onSuccess { continuation.resume(it) }
  onError { continuation.resumeWithException(it) }
  start()
}
