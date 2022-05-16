package app.cash.sqldelight.async

import app.cash.sqldelight.TransactionCallbacks
import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.internal.currentThreadId

/**
 * A transaction-aware [AsyncSqlDriver] wrapper which can begin a [Transaction] on the current connection.
 */
interface AsyncTransacter {
  /**
   * Starts a [Transaction] and runs [bodyWithReturn] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  suspend fun <R> transactionWithResult(
    noEnclosing: Boolean = false,
    bodyWithReturn: suspend AsyncTransactionWithReturn<R>.() -> R
  ): R

  /**
   * Starts a [Transaction] and runs [body] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  suspend fun transaction(
    noEnclosing: Boolean = false,
    body: suspend AsyncTransactionWithoutReturn.() -> Unit
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
    internal var transacter: AsyncTransacter? = null

    /**
     * The parent transaction, if there is any.
     */
    abstract val enclosingTransaction: Transaction?

    internal fun enclosingTransaction() = enclosingTransaction

    /**
     * Signal to the underlying SQL driver that this transaction should be finished.
     *
     * @param successful Whether the transaction completed successfully or not.
     */
    abstract suspend fun endTransaction(successful: Boolean)

    internal suspend fun endTransaction() {
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

interface AsyncTransactionWithReturn<R> : TransactionCallbacks {
  /**
   * Rolls back this transaction.
   */
  fun rollback(returnValue: R): Nothing

  /**
   * Begin an inner transaction.
   */
  suspend fun <R> transactionWithResult(body: suspend AsyncTransactionWithReturn<R>.() -> R): R
}

interface AsyncTransactionWithoutReturn : TransactionCallbacks {
  /**
   * Rolls back this transaction.
   */
  fun rollback(): Nothing

  /**
   * Begin an inner transaction.
   */
  suspend fun transaction(body: suspend AsyncTransactionWithoutReturn.() -> Unit)
}

private class AsyncTransactionWrapper<R>(
  val transaction: AsyncTransacter.Transaction
) : AsyncTransactionWithoutReturn, AsyncTransactionWithReturn<R> {
  override fun rollback(): Nothing {
    transaction.checkThreadConfinement()
    throw RollbackException()
  }

  override fun rollback(returnValue: R): Nothing {
    transaction.checkThreadConfinement()
    throw RollbackException()
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

  override suspend fun transaction(body: suspend AsyncTransactionWithoutReturn.() -> Unit) {
    transaction.transacter!!.transaction(false, body)
  }

  override suspend fun <R> transactionWithResult(body: suspend AsyncTransactionWithReturn<R>.() -> R): R {
    return transaction.transacter!!.transactionWithResult(false, body)
  }
}

internal class RollbackException(val value: Any? = null) : Throwable()

/**
 * A transaction-aware [AsyncSqlDriver] wrapper which can begin a [Transaction] on the current connection.
 */
abstract class AsyncTransacterImpl(private val driver: AsyncSqlDriver) : AsyncTransacter {
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
      driver.notifyListeners(tableKeys.toTypedArray())
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

  override suspend fun transaction(
    noEnclosing: Boolean,
    body: suspend AsyncTransactionWithoutReturn.() -> Unit
  ) {
    return transactionWithWrapper(noEnclosing, body)
  }

  override suspend fun <R> transactionWithResult(noEnclosing: Boolean, bodyWithReturn: suspend AsyncTransactionWithReturn<R>.() -> R): R {
    return transactionWithWrapper(noEnclosing, bodyWithReturn)
  }

  private suspend fun <R> transactionWithWrapper(noEnclosing: Boolean, wrapperBody: suspend AsyncTransactionWrapper<R>.() -> R): R {
    val transaction = driver.newTransaction()
    val enclosing = transaction.enclosingTransaction()

    check(enclosing == null || !noEnclosing) { "Already in a transaction" }

    var thrownException: Throwable? = null
    var returnValue: R? = null

    try {
      transaction.transacter = this
      returnValue = AsyncTransactionWrapper<R>(transaction).wrapperBody()
      transaction.successful = true
    } catch (e: Throwable) {
      thrownException = e
    } finally {
      transaction.endTransaction()
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
            driver.notifyListeners(transaction.pendingTables.toTypedArray())
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
  }
}
