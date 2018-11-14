package com.squareup.sqldelight.driver

import co.touchlab.sqliter.*
import co.touchlab.stately.concurrency.QuickLock
import co.touchlab.stately.concurrency.withLock
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.*
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

class SQLiterHelper(private val databaseManager: DatabaseManager) : SqlDatabase {

  override fun close() {
    databaseManager.close()
  }

  override fun getConnection(): SqlDatabaseConnection =
    SQLiterConnection(databaseManager.createConnection())
}

/**
 * For simplicity with the current API, the connection can be shared between threads. In theory, however,
 * in a "Saner Concurrency" context, we'd be better off if connections were tied to threads.
 *
 * SQLite cannot nest transactions, so there should be no situation where we have an enclosing transaction.
 * Transactions are isolated to connections, though, so if you're looking to have multiple transactions,
 * create multiple connections. This won't help a lot from a performance perspective because only one
 * connection can write at one time.
 */
class SQLiterConnection(
  private val databaseConnection: DatabaseConnection
) : SqlDatabaseConnection {
  private val transaction: AtomicReference<Transaction?> = AtomicReference(null)
  private val transLock = QuickLock()

  override fun currentTransaction(): Transacter.Transaction? = transaction.value

  override fun newTransaction(): Transacter.Transaction =
    transLock.withLock {
      if (transaction.value != null)
        throw IllegalStateException("Transaction already active")
      databaseConnection.beginTransaction()
      val trans = Transaction()
      transaction.value = trans
      return trans
    }

  override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type, parameters: Int): SqlPreparedStatement {
    return SQLiterStatement(databaseConnection.createStatement(sql), type)
  }

  inner class Transaction : Transacter.Transaction() {
    override val enclosingTransaction: Transacter.Transaction? = null

    override fun endTransaction(successful: Boolean) = transLock.withLock {
      if (successful) {
        databaseConnection.setTransactionSuccessful()
        databaseConnection.endTransaction()
      } else {
        databaseConnection.endTransaction()
      }

      transaction.value = null
    }
  }
}

class SQLiterStatement(private val statement: Statement, private val type: SqlPreparedStatement.Type) :
    SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    bytes.freeze()
    statement.bindBlob(index, bytes)
  }

  override fun bindDouble(index: Int, double: Double?) {
    statement.bindDouble(index, double)
  }

  override fun bindLong(index: Int, long: Long?) {
    statement.bindLong(index, long)
  }

  override fun bindString(index: Int, string: String?) {
    statement.bindString(index, string)
  }

  override fun execute() {
    when (type) {
      SqlPreparedStatement.Type.INSERT -> {
        statement.executeInsert()
      }
      SqlPreparedStatement.Type.UPDATE, SqlPreparedStatement.Type.DELETE -> {
        statement.executeUpdateDelete().toLong()
      }
      SqlPreparedStatement.Type.EXECUTE -> {
        statement.execute()
      }
      SqlPreparedStatement.Type.SELECT -> throw AssertionError()
    }
  }

  override fun executeQuery(): SqlCursor {
    return SQLiterCursor(statement)
  }
}

class SQLiterCursor(statement: Statement) : SqlCursor {
  private val cursor = statement.query()

  override fun close() {
    cursor.close()
  }

  override fun getBytes(index: Int): ByteArray? = cursor.getBytesOrNull(index)

  override fun getDouble(index: Int): Double? = cursor.getDoubleOrNull(index)

  override fun getLong(index: Int): Long? = cursor.getLongOrNull(index)

  override fun getString(index: Int): String? = cursor.getStringOrNull(index)

  override fun next(): Boolean = cursor.next()
}