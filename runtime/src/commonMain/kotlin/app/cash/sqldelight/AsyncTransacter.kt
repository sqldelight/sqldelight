package app.cash.sqldelight

import app.cash.sqldelight.db.AsyncSqlDriver
import app.cash.sqldelight.db.SqlDriver

/**
 * A transaction-aware [SqlDriver] wrapper which can begin a [Transaction] on the current connection.
 */
interface AsyncTransacter {
  /**
   * Starts a [Transaction] and runs [bodyWithReturn] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   * TODO: Implement this in extensions, or something
   */
  /*fun <R> transactionWithResult(
          noEnclosing: Boolean = false,
          bodyWithReturn: TransactionWithReturn<R>.() -> R
  ): AsyncSqlDriver.Callback<R>*/

  /**
   * Starts a [Transaction] and runs [body] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  fun transaction(
    noEnclosing: Boolean = false,
    body: AsyncTransactionWithoutReturn.() -> Unit
  ): AsyncSqlDriver.Callback<Unit>

  /**
   * An async SQL transaction. Can be created through the driver via [SqlDriver.newTransaction] or
   * through an implementation of [Transacter] by calling [Transacter.transaction].
   *
   * A transaction is expected never to escape the thread it is created on, or more specifically,
   * never to escape the lambda scope of [Transacter.transaction] and [Transacter.transactionWithResult].
   */
  abstract class Transaction : TransactionBase<Transaction>() {
    internal var transacter: AsyncTransacter? = null

    /**
     * Signal to the underlying SQL driver that this transaction should be finished.
     *
     * @param successful Whether the transaction completed successfully or not.
     */
    protected abstract fun endTransaction(successful: Boolean): AsyncSqlDriver.Callback<Unit>

    internal fun endTransaction(): AsyncSqlDriver.Callback<Unit> {
      checkThreadConfinement()
      return endTransaction(successful && childrenSuccessful)
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
  fun <R> transaction(body: AsyncTransactionWithReturn<R>.() -> AsyncSqlDriver.Callback<R>): AsyncSqlDriver.Callback<R>
}

interface AsyncTransactionWithoutReturn : TransactionCallbacks {
  /**
   * Rolls back this transaction.
   */
  fun rollback(): Nothing

  /**
   * Begin an inner transaction.
   */
  fun transaction(body: AsyncTransactionWithoutReturn.() -> Unit): AsyncSqlDriver.Callback<Unit>
}

private class AsyncTransactionWrapper<R>(
  val transaction: AsyncTransacter.Transaction
) : AsyncTransactionWithoutReturn {
  override fun rollback(): Nothing {
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

  override fun transaction(body: AsyncTransactionWithoutReturn.() -> Unit): AsyncSqlDriver.Callback<Unit> {
    return transaction.transacter!!.transaction(false, body)
  }
}

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

  override fun transaction(
    noEnclosing: Boolean,
    body: AsyncTransactionWithoutReturn.() -> Unit
  ): AsyncSqlDriver.Callback<Unit> {
    return transactionWithWrapper(noEnclosing, body)
  }

  private fun <R> transactionWithWrapper(noEnclosing: Boolean, wrapperBody: AsyncTransactionWrapper<R>.() -> R): AsyncSqlDriver.Callback<R> {
    return AsyncSqlDriver.SimpleCallback { callback ->
      driver.newTransaction().onSuccess { transaction ->
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
            callback.success(thrownException.value as R)
          } else if (thrownException != null) {
            throw thrownException
          } else {
            // We can safely cast to R here because any code path that led here will have set the
            // returnValue to the result of the block
            @Suppress("UNCHECKED_CAST")
            callback.success(returnValue as R)
          }
        }
      }
    }
  }
}
