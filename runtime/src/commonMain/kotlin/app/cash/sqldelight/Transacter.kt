/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight

import app.cash.sqldelight.Transacter.Transaction
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.internal.currentThreadId

interface TransactionWithReturn<R> : TransactionCallbacks {
  /**
   * Rolls back this transaction.
   */
  fun rollback(returnValue: R): Nothing

  /**
   * Begin an inner transaction.
   */
  fun <R> transaction(body: TransactionWithReturn<R>.() -> R): R
}

interface SuspendingTransactionWithReturn<R> : TransactionCallbacks {
  /**
   * Rolls back this transaction.
   */
  fun rollback(returnValue: R): Nothing

  /**
   * Begin an inner transaction.
   */
  suspend fun <R> transaction(body: suspend SuspendingTransactionWithReturn<R>.() -> R): R
}

interface TransactionWithoutReturn : TransactionCallbacks {
  /**
   * Rolls back this transaction.
   */
  fun rollback(): Nothing

  /**
   * Begin an inner transaction.
   */
  fun transaction(body: TransactionWithoutReturn.() -> Unit)
}

interface SuspendingTransactionWithoutReturn : TransactionCallbacks {
  /**
   * Rolls back this transaction.
   */
  fun rollback(): Nothing

  /**
   * Begin an inner transaction.
   */
  suspend fun transactionWithResult(body: suspend SuspendingTransactionWithoutReturn.() -> Unit)
}

sealed interface TransacterBase

/**
 * A transaction-aware [SqlDriver] wrapper which can begin a [Transaction] on the current connection.
 */
interface Transacter : TransacterBase {
  /**
   * Starts a [Transaction] and runs [bodyWithReturn] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  fun <R> transactionWithResult(
    noEnclosing: Boolean = false,
    bodyWithReturn: TransactionWithReturn<R>.() -> R,
  ): R

  /**
   * Starts a [Transaction] and runs [body] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  fun transaction(
    noEnclosing: Boolean = false,
    body: TransactionWithoutReturn.() -> Unit,
  )

  /**
   * A SQL transaction. Can be created through the driver via [SqlDriver.newTransaction] or
   * through an implementation of [Transacter] by calling [Transacter.transaction].
   *
   * A transaction is expected never to escape the thread it is created on, or more specifically,
   * never to escape the lambda scope of [Transacter.transaction] and [Transacter.transactionWithResult].
   */
  abstract class Transaction : TransactionCallbacks {
    private val ownerThreadId = currentThreadId()
    internal val postCommitHooks = mutableListOf<() -> Unit>()

    internal val postRollbackHooks = mutableListOf<() -> Unit>()
    internal val registeredQueries = mutableSetOf<Int>()

    internal val pendingTables = mutableSetOf<String>()
    internal var successful: Boolean = false

    internal var childrenSuccessful: Boolean = true
    internal var transacter: TransacterBase? = null

    /**
     * The parent transaction, if there is any.
     */
    protected abstract val enclosingTransaction: Transaction?

    internal fun enclosingTransaction() = enclosingTransaction

    /**
     * Signal to the underlying SQL driver that this transaction should be finished.
     *
     * @param successful Whether the transaction completed successfully or not.
     */
    protected abstract fun endTransaction(successful: Boolean): QueryResult<Unit>

    internal fun endTransaction(): QueryResult<Unit> {
      checkThreadConfinement()
      return endTransaction(successful && childrenSuccessful)
    }

    /**
     * Queues [function] to be run after this transaction successfully commits.
     */
    override fun afterCommit(function: () -> Unit) {
      checkThreadConfinement()
      postCommitHooks.add(function)
    }

    /**
     * Queues [function] to be run after this transaction rolls back.
     */
    override fun afterRollback(function: () -> Unit) {
      checkThreadConfinement()
      postRollbackHooks.add(function)
    }

    internal fun checkThreadConfinement() = check(ownerThreadId == currentThreadId()) {
      """
        Transaction objects (`TransactionWithReturn` and `TransactionWithoutReturn`) must be used
        only within the transaction lambda scope.
      """.trimIndent()
    }
  }
}

/**
 * A transaction-aware [SqlDriver] wrapper which can begin a [Transaction] on the current connection.
 */
interface SuspendingTransacter : TransacterBase {
  /**
   * Starts a [Transaction] and runs [bodyWithReturn] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  suspend fun <R> transactionWithResult(
    noEnclosing: Boolean = false,
    bodyWithReturn: suspend SuspendingTransactionWithReturn<R>.() -> R,
  ): R

  /**
   * Starts a [Transaction] and runs [body] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  suspend fun transaction(
    noEnclosing: Boolean = false,
    body: suspend SuspendingTransactionWithoutReturn.() -> Unit,
  )
}

private class RollbackException(val value: Any? = null) : Throwable()

private class TransactionWrapper<R>(
  val transaction: Transaction,
) : TransactionWithoutReturn, TransactionWithReturn<R> {
  override fun rollback(): Nothing {
    transaction.checkThreadConfinement()
    throw RollbackException()
  }
  override fun rollback(returnValue: R): Nothing {
    transaction.checkThreadConfinement()
    throw RollbackException(returnValue)
  }

  /**
   * Queues [function] to be run after this transaction successfully commits.
   */
  override fun afterCommit(function: () -> Unit) {
    transaction.afterCommit(function)
  }

  /**
   * Queues [function] to be run after this transaction rolls back.
   */
  override fun afterRollback(function: () -> Unit) {
    transaction.afterRollback(function)
  }

  override fun transaction(body: TransactionWithoutReturn.() -> Unit) {
    (transaction.transacter as Transacter).transaction(false, body)
  }

  override fun <R> transaction(body: TransactionWithReturn<R>.() -> R): R {
    return (transaction.transacter as Transacter).transactionWithResult(false, body)
  }
}

private class SuspendingTransactionWrapper<R>(
  val transaction: Transaction,
) : SuspendingTransactionWithoutReturn, SuspendingTransactionWithReturn<R> {
  override fun rollback(): Nothing {
    transaction.checkThreadConfinement()
    throw RollbackException()
  }
  override fun rollback(returnValue: R): Nothing {
    transaction.checkThreadConfinement()
    throw RollbackException(returnValue)
  }

  /**
   * Queues [function] to be run after this transaction successfully commits.
   */
  override fun afterCommit(function: () -> Unit) {
    transaction.afterCommit(function)
  }

  /**
   * Queues [function] to be run after this transaction rolls back.
   */
  override fun afterRollback(function: () -> Unit) {
    transaction.afterRollback(function)
  }

  override suspend fun transactionWithResult(body: suspend SuspendingTransactionWithoutReturn.() -> Unit) {
    (transaction.transacter as SuspendingTransacter).transaction(false, body)
  }

  override suspend fun <R> transaction(body: suspend SuspendingTransactionWithReturn<R>.() -> R): R {
    return (transaction.transacter as SuspendingTransacter).transactionWithResult(false, body)
  }
}

abstract class BaseTransacterImpl(protected val driver: SqlDriver) {
  protected fun <R> postTransactionCleanup(transaction: Transaction, enclosing: Transaction?, thrownException: Throwable?, returnValue: R?): R {
    if (enclosing == null) {
      if (!transaction.successful || !transaction.childrenSuccessful) {
        // TODO: If this throws, and we threw in [body] then create a composite exception.
        try {
          transaction.postRollbackHooks.forEach { it.invoke() }
        } catch (rollbackException: Throwable) {
          thrownException?.let {
            throw Throwable("Exception while rolling back from an exception.\nOriginal exception: $thrownException\nwith cause ${thrownException.cause}\n\nRollback exception: $rollbackException", rollbackException)
          }
          throw rollbackException
        }
        transaction.postRollbackHooks.clear()
      } else {
        if (transaction.pendingTables.isNotEmpty()) {
          driver.notifyListeners(queryKeys = transaction.pendingTables.toTypedArray())
        }
        transaction.pendingTables.clear()
        transaction.registeredQueries.clear()
        transaction.postCommitHooks.forEach { it.invoke() }
        transaction.postCommitHooks.clear()
      }
    } else {
      enclosing.childrenSuccessful = transaction.successful && transaction.childrenSuccessful
      enclosing.postCommitHooks.addAll(transaction.postCommitHooks)
      enclosing.postRollbackHooks.addAll(transaction.postRollbackHooks)
      enclosing.registeredQueries.addAll(transaction.registeredQueries)
      enclosing.pendingTables.addAll(transaction.pendingTables)
    }

    if (enclosing == null && thrownException is RollbackException) {
      // We can safely cast to R here because the rollback exception is always created with the
      // correct type.
      @Suppress("UNCHECKED_CAST")
      return thrownException.value as R
    } else if (thrownException != null) {
      throw thrownException
    } else {
      // We can safely cast to R here because any code path that led here will have set the
      // returnValue to the result of the block
      @Suppress("UNCHECKED_CAST")
      return returnValue as R
    }
  }

  /**
   * For internal use, notifies the listeners provided by [listenerProvider] that their underlying result set has
   * changed.
   */
  protected fun notifyQueries(identifier: Int, tableProvider: ((String) -> Unit) -> Unit) {
    val transaction = driver.currentTransaction()
    if (transaction != null) {
      if (transaction.registeredQueries.add(identifier)) {
        tableProvider { transaction.pendingTables.add(it) }
      }
    } else {
      val tableKeys = mutableSetOf<String>()
      tableProvider { tableKeys.add(it) }
      driver.notifyListeners(queryKeys = tableKeys.toTypedArray())
    }
  }

  /**
   * For internal use, creates a string in the format (?, ?, ?) where there are [count] offset.
   */
  protected fun createArguments(count: Int): String {
    if (count == 0) return "()"

    return buildString(count + 2) {
      append("(?")
      repeat(count - 1) {
        append(",?")
      }
      append(')')
    }
  }
}

/**
 * A transaction-aware [SqlDriver] wrapper which can begin a [Transaction] on the current connection.
 */
abstract class TransacterImpl(driver: SqlDriver) : BaseTransacterImpl(driver), Transacter {
  override fun transaction(
    noEnclosing: Boolean,
    body: TransactionWithoutReturn.() -> Unit,
  ) {
    transactionWithWrapper<Unit?>(noEnclosing, body)
  }

  override fun <R> transactionWithResult(
    noEnclosing: Boolean,
    bodyWithReturn: TransactionWithReturn<R>.() -> R,
  ): R {
    return transactionWithWrapper(noEnclosing, bodyWithReturn)
  }

  private fun <R> transactionWithWrapper(noEnclosing: Boolean, wrapperBody: TransactionWrapper<R>.() -> R): R {
    val transaction = driver.newTransaction().value
    val enclosing = transaction.enclosingTransaction()

    check(enclosing == null || !noEnclosing) { "Already in a transaction" }

    var thrownException: Throwable? = null
    var returnValue: R? = null

    try {
      transaction.transacter = this
      returnValue = TransactionWrapper<R>(transaction).wrapperBody()
      transaction.successful = true
    } catch (e: Throwable) {
      thrownException = e
    } finally {
      transaction.endTransaction()
      return postTransactionCleanup(transaction, enclosing, thrownException, returnValue)
    }
  }
}

abstract class SuspendingTransacterImpl(driver: SqlDriver) : BaseTransacterImpl(driver), SuspendingTransacter {
  override suspend fun <R> transactionWithResult(
    noEnclosing: Boolean,
    bodyWithReturn: suspend SuspendingTransactionWithReturn<R>.() -> R,
  ): R {
    return transactionWithWrapper(noEnclosing, bodyWithReturn)
  }

  override suspend fun transaction(
    noEnclosing: Boolean,
    body: suspend SuspendingTransactionWithoutReturn.() -> Unit,
  ) {
    return transactionWithWrapper(noEnclosing, body)
  }

  private suspend fun <R> transactionWithWrapper(noEnclosing: Boolean, wrapperBody: suspend SuspendingTransactionWrapper<R>.() -> R): R {
    val transaction = driver.newTransaction().await()
    val enclosing = transaction.enclosingTransaction()

    check(enclosing == null || !noEnclosing) { "Already in a transaction" }

    var thrownException: Throwable? = null
    var returnValue: R? = null

    try {
      transaction.transacter = this
      returnValue = SuspendingTransactionWrapper<R>(transaction).wrapperBody()
      transaction.successful = true
    } catch (e: Throwable) {
      thrownException = e
    } finally {
      transaction.endTransaction().await()
      return postTransactionCleanup(transaction, enclosing, thrownException, returnValue)
    }
  }
}
