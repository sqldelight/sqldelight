package com.squareup.sqldelight.runtime.rx

import com.squareup.sqldelight.Query
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Turns this [Query] into an [Observable] which emits whenever the underlying result set changes.
 *
 * ### Scheduler:
 *   [observe] by default operates on the [io.reactivex.schedulers.Schedulers.io] scheduler but can
 *   be optionally overridden with [scheduler]
 */
@CheckReturnValue
fun <T : Any> Query<T>.observe(scheduler: Scheduler = Schedulers.io()): QueryObservable<T> {
  return QueryObservable(this, scheduler)
}

class QueryObservable<RowType : Any>(
  private val query: Query<RowType>,
  private val scheduler: Scheduler
) : Observable<Query<RowType>>() {
  override fun subscribeActual(observer: Observer<in Query<RowType>>) {
    val listener = Listener(query, observer, scheduler)
    observer.onSubscribe(listener)
    query.addListener(listener)
    listener.queryResultsChanged()
  }

  @CheckReturnValue
  fun mapToOne(): Observable<RowType> {
    return map { it.executeAsOne() }
  }

  @CheckReturnValue
  fun mapToOneOrDefault(defaultValue: RowType): Observable<RowType> {
    return map { it.executeAsOneOrNull() ?: defaultValue }
  }

  @CheckReturnValue
  fun mapToOptional(): Observable<Optional<RowType>> {
    return map { Optional.ofNullable(it.executeAsOneOrNull()) }
  }

  @CheckReturnValue
  fun mapToList(): Observable<List<RowType>> {
    return map { it.executeAsList() }
  }

  @CheckReturnValue
  fun mapToOneNonNull(): Observable<RowType> {
    return flatMap {
      val result = it.executeAsOneOrNull()
      if (result == null) Observable.empty() else Observable.just(result)
    }
  }
}

private class Listener<RowType : Any>(
  private val query: Query<RowType>,
  private val observer: Observer<in Query<RowType>>,
  private val scheduler: Scheduler
) : AtomicBoolean(), Query.Listener, Disposable, Runnable {
  override fun isDisposed() = get()

  override fun dispose() {
    if (compareAndSet(false, true)) {
      query.removeListener(this)
    }
  }

  override fun queryResultsChanged() {
    if (!isDisposed) {
      scheduler.scheduleDirect(this)
    }
  }

  override fun run() {
    observer.onNext(query)
  }
}
