@file:JvmName("RxQuery")

package com.squareup.sqldelight.runtime.rx3

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlCursor
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Turns this [Query] into an [Observable] which emits whenever the underlying result set changes.
 *
 * @param scheduler By default, emissions occur on the [Schedulers.io] scheduler but can be
 * optionally overridden.
 */
@CheckReturnValue
@JvmOverloads
@JvmName("toObservable")
fun <T : Any> Query<T, SqlCursor>.asObservable(scheduler: Scheduler = Schedulers.io()): Observable<Query<T, SqlCursor>> {
  return Observable.create(QueryOnSubscribe(this)).observeOn(scheduler)
}

private class QueryOnSubscribe<T : Any>(
  private val query: Query<T, SqlCursor>
) : ObservableOnSubscribe<Query<T, SqlCursor>> {
  override fun subscribe(emitter: ObservableEmitter<Query<T, SqlCursor>>) {
    val listenerAndDisposable = QueryListenerAndDisposable(emitter, query)
    query.addListener(listenerAndDisposable)
    emitter.setDisposable(listenerAndDisposable)
    emitter.onNext(query)
  }
}

private class QueryListenerAndDisposable<T : Any>(
  private val emitter: ObservableEmitter<Query<T, SqlCursor>>,
  private val query: Query<T, SqlCursor>
) : AtomicBoolean(), Query.Listener, Disposable {
  override fun queryResultsChanged() {
    emitter.onNext(query)
  }

  override fun isDisposed() = get()

  override fun dispose() {
    if (compareAndSet(false, true)) {
      query.removeListener(this)
    }
  }
}

@CheckReturnValue
fun <T : Any> Observable<Query<T, SqlCursor>>.mapToOne(): Observable<T> {
  return map { it.executeAsOne() }
}

@CheckReturnValue
fun <T : Any> Observable<Query<T, SqlCursor>>.mapToOneOrDefault(defaultValue: T): Observable<T> {
  return map { it.executeAsOneOrNull() ?: defaultValue }
}

@CheckReturnValue
fun <T : Any> Observable<Query<T, SqlCursor>>.mapToOptional(): Observable<Optional<T>> {
  return map { Optional.ofNullable(it.executeAsOneOrNull()) }
}

@CheckReturnValue
fun <T : Any> Observable<Query<T, SqlCursor>>.mapToList(): Observable<List<T>> {
  return map { it.executeAsList() }
}

@CheckReturnValue
fun <T : Any> Observable<Query<T, SqlCursor>>.mapToOneNonNull(): Observable<T> {
  return flatMap {
    val result = it.executeAsOneOrNull()
    if (result == null) Observable.empty() else Observable.just(result)
  }
}
