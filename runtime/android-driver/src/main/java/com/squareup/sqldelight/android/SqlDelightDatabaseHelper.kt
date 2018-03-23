package com.squareup.sqldelight.android

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.arch.persistence.db.SupportSQLiteProgram
import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.db.SupportSQLiteStatement
import android.database.Cursor
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXEC
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE
import com.squareup.sqldelight.db.SqlResultSet
import java.util.ArrayDeque
import java.util.Queue

class SqlDelightDatabaseHelper(
  private val openHelper: SupportSQLiteOpenHelper
) : SqlDatabase {
  private val transactions = ThreadLocal<SqlDelightDatabaseConnection.Transaction>()

  override fun getConnection(): SqlDatabaseConnection {
    return SqlDelightDatabaseConnection(openHelper.writableDatabase, transactions)
  }

  override fun close() {
    return openHelper.close()
  }

  class Callback(
    private val helper: SqlDatabase.Helper,
    version: Int
  ) : SupportSQLiteOpenHelper.Callback(version) {
    override fun onCreate(db: SupportSQLiteDatabase) {
      helper.onCreate(SqlDelightDatabaseConnection(db, ThreadLocal()))
    }

    override fun onUpgrade(
      db: SupportSQLiteDatabase,
      oldVersion: Int,
      newVersion: Int
    ) {
      helper.onMigrate(SqlDelightDatabaseConnection(db, ThreadLocal()), oldVersion, newVersion)
    }
  }
}

private class SqlDelightDatabaseConnection(
  private val database: SupportSQLiteDatabase,
  private val transactions: ThreadLocal<Transaction>
) : SqlDatabaseConnection {
  override fun newTransaction(): Transaction {
    val enclosing = transactions.get()
    val transaction = Transaction(enclosing)
    transactions.set(transaction)

    if (enclosing == null) {
      database.beginTransactionNonExclusive()
    }

    return transaction
  }

  override fun currentTransaction() = transactions.get()

  inner class Transaction(
    override val enclosingTransaction: Transaction?
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean) {
      if (enclosingTransaction == null) {
        if (successful) {
          database.setTransactionSuccessful()
          database.endTransaction()
        } else {
          database.endTransaction()
        }
      } else {
        transactions.set(enclosingTransaction)
      }
    }
  }

  override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type) = when(type) {
    SELECT -> SqlDelightQuery(sql, database)
    INSERT, UPDATE, DELETE, EXEC -> SqlDelightPreparedStatement(database.compileStatement(sql), type)
  }
}

private class SqlDelightPreparedStatement(
  private val statement: SupportSQLiteStatement,
  private val type: SqlPreparedStatement.Type
) : SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    if (bytes == null) statement.bindNull(index) else statement.bindBlob(index, bytes)
  }

  override fun bindLong(index: Int, long: Long?) {
    if (long == null) statement.bindNull(index) else statement.bindLong(index, long)
  }

  override fun bindDouble(index: Int, double: Double?) {
    if (double == null) statement.bindNull(index) else statement.bindDouble(index, double)
  }

  override fun bindString(index: Int, string: String?) {
    if (string == null) statement.bindNull(index) else statement.bindString(index, string)
  }

  override fun executeQuery() = throw UnsupportedOperationException()

  override fun execute() = when (type) {
    INSERT -> statement.executeInsert()
    UPDATE, DELETE -> statement.executeUpdateDelete().toLong()
    EXEC -> {
      statement.execute()
      1
    }
    SELECT -> throw AssertionError()
  }

}

private class SqlDelightQuery(
  private val sql: String,
  private val database: SupportSQLiteDatabase
) : SupportSQLiteQuery, SqlPreparedStatement {
  val binds: Queue<(SupportSQLiteProgram) -> Unit> = ArrayDeque()

  override fun bindBytes(index: Int, bytes: ByteArray?) {
    binds.add { if (bytes == null) it.bindNull(index) else it.bindBlob(index, bytes) }
  }

  override fun bindLong(index: Int, long: Long?) {
    binds.add { if (long == null) it.bindNull(index) else it.bindLong(index, long) }
  }

  override fun bindDouble(index: Int, double: Double?) {
    binds.add { if (double == null) it.bindNull(index) else it.bindDouble(index, double) }
  }

  override fun bindString(index: Int, string: String?) {
    binds.add { if (string == null) it.bindNull(index) else it.bindString(index, string) }
  }

  override fun execute() = throw UnsupportedOperationException()

  override fun executeQuery() = SqlDelightResultSet(database.query(this))

  override fun bindTo(statement: SupportSQLiteProgram) {
    synchronized(binds) {
      while (binds.isNotEmpty()) {
        binds.poll().invoke(statement)
      }
    }
  }

  override fun getSql() = sql
}

private class SqlDelightResultSet(
  private val cursor: Cursor
) : SqlResultSet {
  override fun next() = cursor.moveToNext()
  override fun getString(index: Int) = cursor.getString(index)
  override fun getLong(index: Int) = cursor.getLong(index)
  override fun getBytes(index: Int) = cursor.getBlob(index)
  override fun getDouble(index: Int) = cursor.getDouble(index)
  override fun close() = cursor.close()
}