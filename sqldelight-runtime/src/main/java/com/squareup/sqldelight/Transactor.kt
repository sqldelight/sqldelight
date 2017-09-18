package com.squareup.sqldelight

import android.support.annotation.WorkerThread
import java.io.Closeable
import java.util.concurrent.TimeUnit


interface Transactor {
  /**
   * Begin a transaction for this thread.
   *
   * Transactions may nest. If the transaction is not in progress, then a database connection is
   * obtained and a new transaction is started. Otherwise, a nested transaction is started.
   *
   * Each call to [newTransaction] must be matched exactly by a call to [Transaction.end]. To mark a
   * transaction as successful, call [Transaction.markSuccessful] before calling [Transaction.end].
   * If the transaction is not successful, or if any of its nested transactions were not successful,
   * then the entire transaction will be rolled back when the outermost transaction is ended.
   *
   * Transactions queue up all query notifications until they have been applied.
   *
   * Here is the standard idiom for transactions:
   *
   * ```
   * try (Transaction transaction = db.newTransaction()) {
   *   ...
   *   transaction.markSuccessful();
   * }
   * ```
   *
   * Manually call [Transaction.end] when try-with-resources is not available:
   * ```
   * Transaction transaction = db.newTransaction();
   * try {
   *   ...
   *   transaction.markSuccessful();
   * } finally {
   *   transaction.end();
   * }
   * ```
   *
   *
   * @see android.arch.persistence.db.SupportSQLiteDatabase.beginTransaction
   */
  fun newTransaction(): Transaction
  /**
   * Begins a transaction in IMMEDIATE mode for this thread.
   *
   * Transactions may nest. If the transaction is not in progress, then a database connection is
   * obtained and a new transaction is started. Otherwise, a nested transaction is started.
   *
   * Each call to [newNonExclusiveTransaction] must be matched exactly by a call to
   * [Transaction.end]. To mark a transaction as successful, call [Transaction.markSuccessful]
   * before calling [Transaction.end]. If the transaction is not successful, or if any of its nested
   * transactions were not successful, then the entire transaction will be rolled back when the
   * outermost transaction is ended.
   *
   * Transactions queue up all query notifications until they have been applied.
   *
   * Here is the standard idiom for transactions:
   *
   * ```
   * try (Transaction transaction = db.newNonExclusiveTransaction()) {
   *   ...
   *   transaction.markSuccessful();
   * }
   * ```
   *
   * Manually call [Transaction.end] when try-with-resources is not available:
   * ```
   * Transaction transaction = db.newNonExclusiveTransaction();
   * try {
   *   ...
   *   transaction.markSuccessful();
   * } finally {
   *   transaction.end();
   * }
   * ```
   *
   *
   * @see android.arch.persistence.db.SupportSQLiteDatabase.beginTransactionNonExclusive
   */
  fun newNonExclusiveTransaction(): Transaction

  /**
   * Queues [function] to be performed after [Transaction.end] is called on the active
   * transaction for the current thread if [Transaction.markSuccessful] was called. If there is no
   * active transaction, [function] is invoked immediately.
   */
  fun afterTransaction(function: () -> Unit)
}

/** An in-progress database transaction. */
interface Transaction : Closeable {
  /**
   * End a transaction. See [Transactor.newTransaction] for notes about how to use this and when
   * transactions are committed and rolled back.
   *
   * @see android.arch.persistence.db.SupportSQLiteDatabase.endTransaction
   */
  @WorkerThread
  fun end()

  /**
   * Marks the current transaction as successful. Do not do any more database work between
   * calling this and calling [end]. Do as little non-database work as possible in that situation
   * too. If any errors are encountered between this and [end] the transaction will still be
   * committed.
   *
   * @see android.arch.persistence.db.SupportSQLiteDatabase.setTransactionSuccessful
   */
  @WorkerThread
  fun markSuccessful()

  /**
   * Temporarily end the transaction to let other threads run. The transaction is assumed to be
   * successful so far. Do not call [markSuccessful] before calling this. When this returns a new
   * transaction will have been created but not marked as successful. This assumes that there are no
   * nested transactions (newTransaction has only been called once) and will throw an exception if
   * that is not the case.
   *
   * @return true if the transaction was yielded
   *
   * @see android.arch.persistence.db.SupportSQLiteDatabase.yieldIfContendedSafely
   */
  @WorkerThread
  fun yieldIfContendedSafely(): Boolean

  /**
   * Temporarily end the transaction to let other threads run. The transaction is assumed to be
   * successful so far. Do not call [markSuccessful] before calling this. When this returns a new
   * transaction will have been created but not marked as successful. This assumes that there are no
   * nested transactions (newTransaction has only been called once) and will throw an exception if
   * that is not the case.
   *
   * @param sleepAmount if > 0, sleep this long before starting a new transaction if
   *   the lock was actually yielded. This will allow other background threads to make some
   *   more progress than they would if we started the transaction immediately.
   * @return true if the transaction was yielded
   *
   * @see android.arch.persistence.db.SupportSQLiteDatabase.yieldIfContendedSafely
   */
  @WorkerThread
  fun yieldIfContendedSafely(sleepAmount: Long, sleepUnit: TimeUnit): Boolean

  /**
   * Equivalent to calling [end]
   */
  @WorkerThread
  override fun close()
}

