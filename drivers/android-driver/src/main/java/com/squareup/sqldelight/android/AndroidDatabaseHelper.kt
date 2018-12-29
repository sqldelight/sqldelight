package com.squareup.sqldelight.android

import android.content.Context
import android.database.Cursor
import android.util.LruCache
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXECUTE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE
import com.squareup.sqldelight.db.SqlCursor

private val DEFAULT_CACHE_SIZE = 20

class AndroidSqlDatabase private constructor(
  private val openHelper: SupportSQLiteOpenHelper? = null,
  database: SupportSQLiteDatabase? = null,
  private val cacheSize: Int
) : SqlDatabase {
  private val transactions = ThreadLocal<Transacter.Transaction>()
  private val database = openHelper?.writableDatabase ?: database!!

  constructor(
    openHelper: SupportSQLiteOpenHelper
  ) : this(openHelper = openHelper, database = null, cacheSize = DEFAULT_CACHE_SIZE)

  /**
   * @param [cacheSize] The number of compiled sqlite statements to keep in memory per connection.
   *   Defaults to 20.
   */
  @JvmOverloads constructor(
    schema: SqlDatabase.Schema,
    context: Context,
    name: String? = null,
    factory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
    callback: SupportSQLiteOpenHelper.Callback = AndroidSqlDatabase.Callback(schema),
    cacheSize: Int = DEFAULT_CACHE_SIZE
  ) : this(
      database = null,
      openHelper = factory.create(SupportSQLiteOpenHelper.Configuration.builder(context)
          .callback(callback)
          .name(name)
          .build()),
      cacheSize = cacheSize
  )

  constructor(
    database: SupportSQLiteDatabase
  ) : this(openHelper = null, database = database, cacheSize = DEFAULT_CACHE_SIZE)

  private val statements = object : LruCache<Int, SqlPreparedStatement>(cacheSize) {
    override fun entryRemoved(
      evicted: Boolean,
      key: Int,
      oldValue: SqlPreparedStatement,
      newValue: SqlPreparedStatement?
    ) {
      if (oldValue is AndroidPreparedStatement) oldValue.close()
    }
  }

  override fun newTransaction(): Transacter.Transaction {
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
    override val enclosingTransaction: Transacter.Transaction?
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
    identifier: Int?,
    sql: String,
    type: SqlPreparedStatement.Type,
    parameters: Int
  ): SqlPreparedStatement {
    if (identifier != null) statements.get(identifier)?.let { return it }
    val statement = when(type) {
      SELECT -> AndroidQuery(sql, database, parameters)
      INSERT, UPDATE, DELETE, EXECUTE -> AndroidPreparedStatement(database.compileStatement(sql), type)
    }
    if (identifier != null) statements.put(identifier, statement)
    return statement
  }

  override fun close() {
    if (openHelper == null) {
      throw IllegalStateException("Tried to call close during initialization")
    }
    return openHelper.close()
  }

  open class Callback(
    private val schema: SqlDatabase.Schema
  ) : SupportSQLiteOpenHelper.Callback(schema.version) {
    override fun onCreate(db: SupportSQLiteDatabase) {
      schema.create(AndroidSqlDatabase(openHelper = null, database = db, cacheSize = 1))
    }

    override fun onUpgrade(
      db: SupportSQLiteDatabase,
      oldVersion: Int,
      newVersion: Int
    ) {
      schema.migrate(AndroidSqlDatabase(openHelper = null, database = db, cacheSize = 1), oldVersion, newVersion)
    }
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

  override fun execute() {
    when (type) {
      SELECT -> throw AssertionError()
      else -> statement.execute()
    }
  }

  fun close() {
    statement.close()
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

  override fun executeQuery() = AndroidCursor(database.query(this))

  override fun bindTo(statement: SupportSQLiteProgram) {
    for (action in binds.values) {
      action(statement)
    }
  }

  override fun getSql() = sql

  override fun toString() = sql

  override fun getArgCount() = argCount
}

private class AndroidCursor(
  private val cursor: Cursor
) : SqlCursor {
  override fun next() = cursor.moveToNext()
  override fun getString(index: Int) = if (cursor.isNull(index)) null else cursor.getString(index)
  override fun getLong(index: Int) = if (cursor.isNull(index)) null else cursor.getLong(index)
  override fun getBytes(index: Int) = if (cursor.isNull(index)) null else cursor.getBlob(index)
  override fun getDouble(index: Int) = if (cursor.isNull(index)) null else cursor.getDouble(index)
  override fun close() = cursor.close()
}
