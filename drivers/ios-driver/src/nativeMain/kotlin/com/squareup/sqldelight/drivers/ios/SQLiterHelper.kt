package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.bindLong
import co.touchlab.sqliter.bindDouble
import co.touchlab.sqliter.bindString
import co.touchlab.sqliter.bindBlob
import co.touchlab.sqliter.getBytesOrNull
import co.touchlab.sqliter.getDoubleOrNull
import co.touchlab.sqliter.getLongOrNull
import co.touchlab.sqliter.getStringOrNull
import co.touchlab.stately.collections.frozenHashMap
import co.touchlab.stately.collections.frozenLinkedList
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.QuickLock
import co.touchlab.stately.concurrency.withLock
import co.touchlab.stately.freeze
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXECUTE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE

class SQLiterHelper(private val databaseManager: DatabaseManager) : SqlDatabase {
  private val enforceClosed = EnforceClosed()

  private val connection: SQLiterConnection by lazy {
    SQLiterConnection(databaseManager.createConnection())
  }

  override fun close() {
    enforceClosed.checkNotClosed()
    enforceClosed.trackClosed()
    connection.close()
    databaseManager.close()
  }

  override fun getConnection(): SqlDatabaseConnection {
    enforceClosed.checkNotClosed()
    return connection
  }
}

class SQLiterConnection(
  private val databaseConnection: DatabaseConnection
) : SqlDatabaseConnection {
  private val enforceClosed = EnforceClosed()
  private val transaction: AtomicReference<Transaction?> = AtomicReference(null)
  private val transLock = QuickLock()
  private val statementList = frozenLinkedList<Statement>(stableIterator = false)

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

  override fun prepareStatement(
    sql: String,
    type: SqlPreparedStatement.Type,
    parameters: Int
  ): SqlPreparedStatement {
    enforceClosed.checkNotClosed()
    val statement = databaseConnection.createStatement(sql)
    statementList.add(statement)
    return when (type) {
      SELECT -> SQLiterQuery(statement)
      INSERT, UPDATE, DELETE, EXECUTE -> SQLiterStatement(statement)
    }
  }

  internal fun close() {
    enforceClosed.checkNotClosed()
    enforceClosed.trackClosed()
    statementList.forEach { it.finalizeStatement() }
    databaseConnection.close()
  }

  private inner class Transaction : Transacter.Transaction() {
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

private class SQLiterQuery(
  private val statement: Statement
) : SqlPreparedStatement {
  private val binds = frozenHashMap<Int, (Statement) -> Unit>()

  override fun bindBytes(
    index: Int,
    bytes: ByteArray?
  ) {
    bytes.freeze()
    binds.put(index) { it.bindBlob(index, bytes) }
  }

  override fun bindDouble(
    index: Int,
    double: Double?
  ) {
    binds.put(index) { it.bindDouble(index, double) }
  }

  override fun bindLong(
    index: Int,
    long: Long?
  ) {
    binds.put(index) { it.bindLong(index, long) }
  }

  override fun bindString(
    index: Int,
    string: String?
  ) {
    binds.put(index) { it.bindString(index, string) }
  }

  private fun bindTo(statement: Statement) {
    for (bind in binds.values.iterator()) {
      bind(statement)
    }
  }

  override fun execute() = throw UnsupportedOperationException()

  override fun executeQuery(): SqlCursor {
    bindTo(statement)
    return SQLiterCursor(statement.query())
  }
}

class SQLiterStatement(
  private val statement: Statement
) : SqlPreparedStatement {

  override fun bindBytes(
    index: Int,
    bytes: ByteArray?
  ) {
    bytes.freeze()
    statement.bindBlob(index, bytes)
  }

  override fun bindDouble(
    index: Int,
    double: Double?
  ) {
    statement.bindDouble(index, double)
  }

  override fun bindLong(
    index: Int,
    long: Long?
  ) {
    statement.bindLong(index, long)
  }

  override fun bindString(
    index: Int,
    string: String?
  ) {
    statement.bindString(index, string)
  }

  override fun execute() {
    statement.execute()
  }

  override fun executeQuery(): SqlCursor = throw UnsupportedOperationException()
}

private class SQLiterCursor(
  private val cursor: Cursor
) : SqlCursor {

  override fun close() {
    cursor.close()
  }

  override fun getBytes(index: Int): ByteArray? = cursor.getBytesOrNull(index)

  override fun getDouble(index: Int): Double? = cursor.getDoubleOrNull(index)

  override fun getLong(index: Int): Long? = cursor.getLongOrNull(index)

  override fun getString(index: Int): String? = cursor.getStringOrNull(index)

  override fun next(): Boolean = cursor.next()
}

private class EnforceClosed {
  private val closed = AtomicBoolean(false)

  fun trackClosed() {
    closed.value = true
  }

  fun checkNotClosed() {
    if (closed.value) throw IllegalStateException("Closed")
  }
}