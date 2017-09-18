package com.squareup.sqldelight

import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.database.sqlite.SQLiteTransactionListener
import java.io.Closeable
import java.util.LinkedHashSet
import java.util.concurrent.TimeUnit

/**
 * A transaction-aware [SupportSQLiteOpenHelper] wrapper which can queue work to be performed after
 * the active [Transaction] ends with [afterTransaction].
 */
abstract class SqlDelightDatabase(
    internal val helper: SupportSQLiteOpenHelper
) : Transactor, Closeable {

  private val transactions = ThreadLocal<SqliteTransaction>()

  private val transaction = object : Transaction {
    override fun markSuccessful() {
      helper.writableDatabase.setTransactionSuccessful()
    }

    override fun yieldIfContendedSafely(): Boolean {
      return helper.writableDatabase.yieldIfContendedSafely()
    }

    override fun yieldIfContendedSafely(sleepAmount: Long, sleepUnit: TimeUnit): Boolean {
      return helper.writableDatabase.yieldIfContendedSafely(sleepUnit.toMillis(sleepAmount))
    }

    override fun end() {
      val transaction = transactions.get() ?: throw IllegalStateException("Not in transaction.")
      transactions.set(transaction.parent)
      helper.writableDatabase.endTransaction()
      if (transaction.commit) {
        transaction.forEach { function -> afterTransaction(function) }
      }
    }

    override fun close() {
      end()
    }
  }

  override fun newTransaction(): Transaction {
    val transaction = SqliteTransaction(transactions.get())
    transactions.set(transaction)
    helper.writableDatabase.beginTransactionWithListener(transaction)

    return this.transaction
  }

  override fun newNonExclusiveTransaction(): Transaction {
    val transaction = SqliteTransaction(transactions.get())
    transactions.set(transaction)
    helper.writableDatabase.beginTransactionWithListenerNonExclusive(transaction)

    return this.transaction
  }

  override fun afterTransaction(function: () -> Unit) {
    val transaction = transactions.get()
    if (transaction != null) {
      transaction.add(function)
    } else {
      function()
    }
  }

  override fun close() {
    helper.close()
  }
}

private class SqliteTransaction(
    val parent: SqliteTransaction?
) : LinkedHashSet<() -> Unit>(), SQLiteTransactionListener {
  var commit: Boolean = false

  override fun onBegin() {}

  override fun onCommit() {
    commit = true
  }

  override fun onRollback() {}

  override fun toString(): String {
    val name = String.format("%08x", System.identityHashCode(this))
    return if (parent == null) name else name + " [" + parent.toString() + ']'
  }
}