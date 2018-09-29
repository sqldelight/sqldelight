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
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.internal.QueryList

/**
 * A transaction-aware [SqlDatabase] wrapper which can begin a [Transaction] on the current connection.
 */
abstract class Transacter(private val helper: SqlDatabase) {
  /**
   * For internal use, performs [function] immediately if there is no active [Transaction] on this
   * thread, otherwise defers [function] to happen on transaction commit.
   */
  protected fun notifyQueries(queryList: QueryList) {
    val transaction = helper.getConnection().currentTransaction()
    if (transaction != null) {
      transaction.queriesToUpdate.addAll(queryList.queries)
    } else {
      queryList.queries.forEach { it.notifyResultSetChanged() }
    }
  }

  protected fun createArguments(count: Int, offset: Int): String {
    val builder = StringBuilder(count * 3 + 2).apply {
      append("(?").append(offset)
    }
    for (index in offset + 1 until count + offset) {
      builder.append(",?").append(index)
    }
    return builder.append(')').toString()
  }

  /**
   * Starts a [Transaction] and runs [body] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  fun transaction(noEnclosing: Boolean = false, body: Transaction.() -> Unit) {
    val transaction = helper.getConnection().newTransaction()
    val enclosing = transaction.enclosingTransaction()

    if (enclosing != null && noEnclosing) {
      throw IllegalStateException("Already in a transaction")
    }

    try {
      transaction.transacter = this
      transaction.body()
      transaction.successful = true
    } catch (e: RollbackException) {
      if (enclosing != null) throw e
    } finally {
      transaction.endTransaction()
      if (enclosing == null) {
        if (!transaction.successful || !transaction.childrenSuccessful) {
          // TODO: If this throws, and we threw in [body] then create a composite exception.
          transaction.postRollbackHooks.forEach { it.invoke() }
          transaction.postRollbackHooks.clear()
        } else {
          transaction.queriesToUpdate.forEach { it.notifyResultSetChanged() }
          transaction.queriesToUpdate.clear()
          transaction.postCommitHooks.forEach { it.invoke() }
          transaction.postCommitHooks.clear()
        }
      } else {
        enclosing.childrenSuccessful = transaction.successful && transaction.childrenSuccessful
        enclosing.postCommitHooks.addAll(transaction.postCommitHooks)
        enclosing.postRollbackHooks.addAll(transaction.postRollbackHooks)
        enclosing.queriesToUpdate.addAll(transaction.queriesToUpdate)
      }
    }
  }

  abstract class Transaction {
    internal val postCommitHooks: LinkedHashSet<() -> Unit> = LinkedHashSet()
    internal val postRollbackHooks: LinkedHashSet<() -> Unit> = LinkedHashSet()
    internal val queriesToUpdate: LinkedHashSet<Query<*>> = LinkedHashSet()

    internal var successful = false
    internal var childrenSuccessful = true

    internal lateinit var transacter: Transacter

    protected abstract val enclosingTransaction: Transaction?

    internal fun enclosingTransaction() = enclosingTransaction

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
      postCommitHooks.add(function)
    }

    /**
     * Queues [function] to be run after this transaction rolls back.
     */
    fun afterRollback(function: () -> Unit) {
      postRollbackHooks.add(function)
    }

    /**
     * Begin an inner transaction.
     */
    fun transaction(body: Transaction.() -> Unit) = transacter.transaction(false, body)
  }

  private class RollbackException : Throwable()
}
