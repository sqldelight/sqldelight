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

import co.touchlab.stately.collections.SharedSet
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.freeze
import com.squareup.sqldelight.Transacter.Transaction
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.internal.QueryList

/**
 * A transaction-aware [SqlDatabase] wrapper which can begin a [Transaction] on the current connection.
 */
abstract class Transacter(private val database: SqlDatabase) {
  init {
    freeze()
  }

  /**
   * For internal use, performs [function] immediately if there is no active [Transaction] on this
   * thread, otherwise defers [function] to happen on transaction commit.
   */
  protected fun notifyQueries(queryList: QueryList) {
    val transaction = database.getConnection()
        .currentTransaction()
    if (transaction != null) {
      transaction.queriesToUpdate.addAll(queryList.queries)
    } else {
      queryList.queries.forEach { it.notifyDataChanged() }
    }
  }

  protected fun createArguments(
    count: Int,
    offset: Int
  ): String {
    if (count == 0) return "()"

    return buildString(count * 3 + 2) {
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
  fun transaction(
    noEnclosing: Boolean = false,
    body: Transaction.() -> Unit
  ) {
    val transaction = database.getConnection()
        .newTransaction()
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
            transaction.postRollbackHooks.forEach { it.value!!.invoke() }
          } catch (rollbackException: Throwable) {
            thrownException?.let {
              throw Throwable("Exception while rolling back from an exception.\nOriginal exception: $thrownException\nwith cause ${thrownException.cause}\n\nRollback exception: $rollbackException", rollbackException)
            }
            throw rollbackException
          }
          transaction.postRollbackHooks.clear()
        } else {
          transaction.queriesToUpdate.forEach { it.notifyDataChanged() }
          transaction.queriesToUpdate.clear()
          transaction.postCommitHooks.forEach { it.value!!.invoke() }
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
    internal val postCommitHooks: SharedSet<ThreadLocalRef<() -> Unit>> = SharedSet()
    internal val postRollbackHooks: SharedSet<ThreadLocalRef<() -> Unit>> = SharedSet()
    internal val queriesToUpdate: SharedSet<Query<*>> = SharedSet()

    private val atomicSuccessful = AtomicBoolean(false)
    internal var successful: Boolean
      get() = atomicSuccessful.value
      set(value) {
        atomicSuccessful.value = value
      }

    private val atomicChildrenSuccessful = AtomicBoolean(true)
    internal var childrenSuccessful: Boolean
      get() = atomicChildrenSuccessful.value
      set(value) {
        atomicChildrenSuccessful.value = value
      }

    private val atomicTransacter = AtomicReference<Transacter?>(null)
    internal var transacter: Transacter
      get() = atomicTransacter.value!!
      set(value) {
        atomicTransacter.value = value
      }

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
      val threadLocalRef = ThreadLocalRef<() -> Unit>()
      threadLocalRef.value = function
      postCommitHooks.add(threadLocalRef)
    }

    /**
     * Queues [function] to be run after this transaction rolls back.
     */
    fun afterRollback(function: () -> Unit) {
      val threadLocalRef = ThreadLocalRef<() -> Unit>()
      threadLocalRef.value = function
      postRollbackHooks.add(threadLocalRef)
    }

    /**
     * Begin an inner transaction.
     */
    fun transaction(body: Transaction.() -> Unit) = transacter.transaction(false, body)
  }

  private class RollbackException : Throwable()
}