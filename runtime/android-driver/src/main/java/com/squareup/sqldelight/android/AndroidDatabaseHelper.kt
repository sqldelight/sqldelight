@file:JvmName("SqlDelight")
package com.squareup.sqldelight.android

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
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

class AndroidSqlDatabase(
  private val openHelper: SupportSQLiteOpenHelper
) : SqlDatabase {
  private val transactions = ThreadLocal<AndroidDatabaseConnection.Transaction>()

  override fun getConnection(): SqlDatabaseConnection {
    return AndroidDatabaseConnection(openHelper.writableDatabase, transactions)
  }

  override fun close() {
    return openHelper.close()
  }

  open class Callback(
    private val helper: SqlDatabase.Helper
  ) : SupportSQLiteOpenHelper.Callback(helper.version) {
    override fun onCreate(db: SupportSQLiteDatabase) {
      helper.onCreate(AndroidDatabaseConnection(db, ThreadLocal()))
    }

    override fun onUpgrade(
      db: SupportSQLiteDatabase,
      oldVersion: Int,
      newVersion: Int
    ) {
      helper.onMigrate(AndroidDatabaseConnection(db, ThreadLocal()), oldVersion, newVersion)
    }
  }
}

/**
 * Wraps [database] into a [SqlDatabase] usable by a SqlDelight generated QueryWrapper.
 */
fun SqlDatabase.Helper.create(
  database: SupportSQLiteDatabase
): SqlDatabase {
  return AndroidInitializationDatabase(database)
}

/**
 * Wraps [context] into a [SqlDatabase] usable by a SqlDelight generated QueryWrapper.
 */
@JvmOverloads
fun SqlDatabase.Helper.create(
  context: Context,
  name: String? = null,
  callback: SupportSQLiteOpenHelper.Callback = AndroidSqlDatabase.Callback(this)
): SqlDatabase {
  val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
      .callback(callback)
      .name(name)
      .build()
  return AndroidSqlDatabase(FrameworkSQLiteOpenHelperFactory().create(configuration))
}

private class AndroidInitializationDatabase(
  private val database: SupportSQLiteDatabase
) : SqlDatabase {
  override fun getConnection(): SqlDatabaseConnection {
    return AndroidDatabaseConnection(database, ThreadLocal())
  }

  override fun close() {
    throw IllegalStateException("Tried to call close during initialization")
  }
}

private class AndroidDatabaseConnection(
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
      }
      transactions.set(enclosingTransaction)
    }
  }

  override fun prepareStatement(
    sql: String,
    type: SqlPreparedStatement.Type,
    parameters: Int
  ) = when(type) {
    SELECT -> AndroidQuery(sql, database, parameters)
    INSERT, UPDATE, DELETE, EXEC -> AndroidPreparedStatement(database.compileStatement(sql), type)
  }
}

private class AndroidPreparedStatement(
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

private class AndroidQuery(
  private val sql: String,
  private val database: SupportSQLiteDatabase,
  private val argCount: Int
) : SupportSQLiteQuery, SqlPreparedStatement {
  private val binds: MutableMap<Int, (SupportSQLiteProgram) -> Unit> = LinkedHashMap()

  override fun bindBytes(index: Int, bytes: ByteArray?) {
    binds[index] = { if (bytes == null) it.bindNull(index) else it.bindBlob(index, bytes) }
  }

  override fun bindLong(index: Int, long: Long?) {
    binds[index] = { if (long == null) it.bindNull(index) else it.bindLong(index, long) }
  }

  override fun bindDouble(index: Int, double: Double?) {
    binds[index] = { if (double == null) it.bindNull(index) else it.bindDouble(index, double) }
  }

  override fun bindString(index: Int, string: String?) {
    binds[index] = { if (string == null) it.bindNull(index) else it.bindString(index, string) }
  }

  override fun execute() = throw UnsupportedOperationException()

  override fun executeQuery() = AndroidResultSet(database.query(this))

  override fun bindTo(statement: SupportSQLiteProgram) {
    for (action in binds.values) {
      action(statement)
    }
  }

  override fun getSql() = sql

  override fun toString() = sql

  override fun getArgCount() = argCount
}

private class AndroidResultSet(
  private val cursor: Cursor
) : SqlResultSet {
  override fun next() = cursor.moveToNext()
  override fun getString(index: Int) = if (cursor.isNull(index)) null else cursor.getString(index)
  override fun getLong(index: Int) = if (cursor.isNull(index)) null else cursor.getLong(index)
  override fun getBytes(index: Int) = if (cursor.isNull(index)) null else cursor.getBlob(index)
  override fun getDouble(index: Int) = if (cursor.isNull(index)) null else cursor.getDouble(index)
  override fun close() = cursor.close()
}
