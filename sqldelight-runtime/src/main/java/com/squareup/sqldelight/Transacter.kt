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

/**
 * A transaction-aware [SqlDatabase] wrapper which can begin a [Transaction] on the current thread.
 */
abstract class Transacter(
  private val helper: SqlDatabase,
  private val transactions: ThreadLocal<Transaction>
) {
  /**
   * For internal use, performs [function] immediately if there is no active [Transaction] on this
   * thread, otherwise defers [function] to happen on transaction commit.
   */
  protected fun deferAction(function: () -> Unit) {
    val transaction = transactions.get()
    if (transaction != null) {
      transaction.postCommitHooks.add(function)
    } else {
      function()
    }
  }

  /**
   * Starts a [Transaction] and runs [body] in that transaction.
   *
   * @throws IllegalStateException if [noEnclosing] is true and there is already an active
   *   [Transaction] on this thread.
   */
  fun transaction(noEnclosing: Boolean = false, body: Transaction.() -> Unit) {
    val enclosing = transactions.get()
    val transaction = Transaction()

    if (enclosing == null) {
      helper.getConnection().beginTransaction()
    } else if (noEnclosing) {
      throw IllegalStateException("Already in a transaction")
    }

    try {
      transactions.set(transaction)
      transaction.body()
      transaction.successful = true
    } catch (e: RollbackException) {
      if (enclosing != null) throw e
    } finally {
      transactions.set(enclosing)
      if (enclosing == null) {
        if (!transaction.successful || !transaction.childrenSuccessful) {
          helper.getConnection().rollbackTransaction()
          while (transaction.postRollbackHooks.isNotEmpty()) {
            // TODO: If this throws, and we threw in [body] then create a composite exception.
            transaction.postRollbackHooks.removeAt(0).invoke()
          }
        } else {
          helper.getConnection().commitTransaction()
          while (transaction.postCommitHooks.isNotEmpty()) {
            transaction.postCommitHooks.removeAt(0).invoke()
          }
        }
      } else {
        enclosing.childrenSuccessful = transaction.successful && transaction.childrenSuccessful
        enclosing.postCommitHooks.addAll(transaction.postCommitHooks)
        enclosing.postRollbackHooks.addAll(transaction.postRollbackHooks)
      }
    }
  }

  inner class Transaction {
    internal val postCommitHooks: ArrayList<() -> Unit> = ArrayList()
    internal val postRollbackHooks: ArrayList<() -> Unit> = ArrayList()

    internal var successful = false
    internal var childrenSuccessful = true

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
    fun transaction(body: Transaction.() -> Unit) = this@Transacter.transaction(false, body)
  }

  private class RollbackException : RuntimeException()
}