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
package com.squareup.sqldelight

import com.squareup.sqldelight.Transacter.Transaction
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.Atomic
import com.squareup.sqldelight.internal.AtomicBoolean
import com.squareup.sqldelight.internal.Supplier
import com.squareup.sqldelight.internal.getValue
import com.squareup.sqldelight.internal.presizeArguments
import com.squareup.sqldelight.internal.setValue
import com.squareup.sqldelight.internal.sharedSet
import com.squareup.sqldelight.internal.threadLocalRef
import com.squareup.sqldelight.internal.sharedMap

private fun Supplier<() -> Unit>.run() = invoke().invoke()
private fun Supplier<() -> List<Query<*>>>.run() = invoke().invoke()

/**
 * A transaction-aware [SqlDriver] wrapper which can begin a [Transaction] on the current connection.
 */
interface Transacter {
  fun transaction(
    noEnclosing: Boolean = false,
    body: Transaction.() -> Unit
  )

  /**
   * A SQL transaction. Can be created through the driver via [SqlDriver.newTransaction] or
   * through an implementation of [Transacter] by calling [Transacter.transaction].
   */
  abstract class Transaction {
    internal val postCommitHooks = sharedSet<Supplier<() -> Unit>>()
    internal val postRollbackHooks = sharedSet<Supplier<() -> Unit>>()
    internal val queriesFuncs = sharedMap<Int, Supplier<() -> List<Query<*>>>>()

    internal var successful: Boolean by AtomicBoolean(false)
    internal var childrenSuccessful: Boolean by AtomicBoolean(true)
    internal var transacter: Transacter? by Atomic<Transacter?>(null)

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
    protected abstract fun endTransaction(successful: Boolean)

    internal fun endTransaction() = endTransaction(successful && childrenSuccessful)

    /**
     * Rolls back this transaction.
     */
    fun rollback(): Nothing = throw RollbackException()

    /**
     * Queues [function] to be run after this transaction successfully commits.
     */
    fun afterCommit(function: () -> Unit) {
      postCommitHooks.add(threadLocalRef(function))
    }

    /**
     * Queues [function] to be run after this transaction rolls back.
     */
    fun afterRollback(function: () -> Unit) {
      postRollbackHooks.add(threadLocalRef(function))
    }

    /**
     * Begin an inner transaction.
     */
    fun transaction(body: Transaction.() -> Unit) = transacter!!.transaction(false, body)
  }
}

private class RollbackException : Throwable()

/**
 * A transaction-aware [SqlDriver] wrapper which can begin a [Transaction] on the current connection.
 */
abstract class TransacterImpl(private val driver: SqlDriver) : Transacter {
  /**
   * For internal use, notifies the listeners of [queryList] that their underlying result set has
   * changed.
   */
  protected fun notifyQueries(identifier: Int, queryList: () -> List<Query<*>>) {
    val transaction = driver.currentTransaction()
    if (transaction != null) {
      if (!transaction.queriesFuncs.containsKey(identifier)) {
        transaction.queriesFuncs[identifier] = threadLocalRef(queryList)
      }
    } else {
      queryList.invoke().forEach { it.notifyDataChanged() }
    }
  }

  /**
   * For internal use, creates a string in the format (?3, ?4, ?5) where the first index is [offset]
   *   and there are [count] total indexes.
   */
  protected fun createArguments(
    count: Int,
    offset: Int
  ): String {
    if (count == 0) return "()"

    return buildString(presizeArguments(count, offset)) {
      append("(?")
      append(offset)
      for (value in offset + 1 until offset + count) {
        append(",?")
        append(value)
      }
      append(')')
    }
  }

  /**
   * Starts a [Transaction] and runs [body] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  override fun transaction(
    noEnclosing: Boolean,

    body: Transaction.() -> Unit
  ) {
    val transaction = driver.newTransaction()
    val enclosing = transaction.enclosingTransaction()

    if (enclosing != null && noEnclosing) {
      throw IllegalStateException("Already in a transaction")
    }

    var thrownException: Throwable? = null

    try {
      transaction.transacter = this
      transaction.body()
      transaction.successful = true
    } catch (e: RollbackException) {
      if (enclosing != null) throw e
      thrownException = e
    } catch (e: Throwable) {
      thrownException = e
    } finally {
      transaction.endTransaction()
      if (enclosing == null) {
        if (!transaction.successful || !transaction.childrenSuccessful) {
          // TODO: If this throws, and we threw in [body] then create a composite exception.
          try {
            transaction.postRollbackHooks.forEach { it.run() }
          } catch (rollbackException: Throwable) {
            thrownException?.let {
              throw Throwable("Exception while rolling back from an exception.\nOriginal exception: $thrownException\nwith cause ${thrownException.cause}\n\nRollback exception: $rollbackException", rollbackException)
            }
            throw rollbackException
          }
          transaction.postRollbackHooks.clear()
        } else {
          transaction.queriesFuncs
                  .flatMap { (_, queryListSupplier) -> queryListSupplier.run()}
                  .distinct()
                  .forEach { it.notifyDataChanged() }

          transaction.queriesFuncs.clear()
          transaction.postCommitHooks.forEach { it.run() }
          transaction.postCommitHooks.clear()
        }
      } else {
        enclosing.childrenSuccessful = transaction.successful && transaction.childrenSuccessful
        enclosing.postCommitHooks.addAll(transaction.postCommitHooks)
        enclosing.postRollbackHooks.addAll(transaction.postRollbackHooks)
        enclosing.queriesFuncs.putAll(transaction.queriesFuncs)
      }

      if (thrownException != null && thrownException !is RollbackException) {
        throw thrownException
      }
    }
  }
}