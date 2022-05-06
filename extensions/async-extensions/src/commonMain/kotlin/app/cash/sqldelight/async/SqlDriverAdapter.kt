@file:JvmName("SqlDriverAdapter")

package app.cash.sqldelight.async

import app.cash.sqldelight.Query
import app.cash.sqldelight.async.db.AsyncSqlCursor
import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.async.db.AsyncSqlPreparedStatement
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

@JvmOverloads
@JvmName("fromSqlDriver")
fun SqlDriver.asAsyncSqlDriver(
  cursorAdapter: (SqlCursor) -> AsyncSqlCursor = { SqlDriverAdapter.SqlCursorAdapter(it) },
  binderAdapter: (SqlPreparedStatement) -> AsyncSqlPreparedStatement = { SqlDriverAdapter.SqlPreparedStatementAdapter(it) }
): AsyncSqlDriver = SqlDriverAdapter(this, cursorAdapter, binderAdapter)

internal class SqlDriverAdapter(
  private val driver: SqlDriver,
  private val cursorAdapter: (SqlCursor) -> AsyncSqlCursor,
  private val binderAdapter: (SqlPreparedStatement) -> AsyncSqlPreparedStatement
) : AsyncSqlDriver {
  private val listeners = mutableMapOf<AsyncQuery.Listener, Query.Listener>()

  override suspend fun close() {
    driver.close()
  }

  override suspend fun <R> executeQuery(identifier: Int?, sql: String, mapper: (AsyncSqlCursor) -> R, parameters: Int, binders: (AsyncSqlPreparedStatement.() -> Unit)?): R {
    return driver.executeQuery(identifier, sql, { mapper(cursorAdapter(it)) }, parameters, binders?.let { { binderAdapter(this).binders() } })
  }

  override suspend fun execute(identifier: Int?, sql: String, parameters: Int, binders: (AsyncSqlPreparedStatement.() -> Unit)?): Long {
    return driver.execute(identifier, sql, parameters, binders?.let { { binderAdapter(this).binders() } })
  }

  override suspend fun newTransaction(): AsyncTransacter.Transaction {
    TODO("Not yet implemented")
  }

  override fun currentTransaction(): AsyncTransacter.Transaction? {
    TODO("Not yet implemented")
  }

  override fun addListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) {
    val syncListener = object : Query.Listener {
      override fun queryResultsChanged() {
        listener.queryResultsChanged()
      }
    }

    listeners[listener] = syncListener
    driver.addListener(syncListener, queryKeys)
  }

  override fun removeListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) {
    val syncListener = listeners.getOrElse(listener) {
      object : Query.Listener {
        override fun queryResultsChanged() = Unit
      }
    }

    driver.removeListener(syncListener, queryKeys)
  }

  override fun notifyListeners(queryKeys: Array<String>) {
    driver.notifyListeners(queryKeys)
  }

  internal class SqlCursorAdapter(private val cursor: SqlCursor) : AsyncSqlCursor {
    override fun next(): Boolean = cursor.next()

    override fun getString(index: Int): String? = cursor.getString(index)

    override fun getLong(index: Int): Long? = cursor.getLong(index)

    override fun getBytes(index: Int): ByteArray? = cursor.getBytes(index)

    override fun getDouble(index: Int): Double? = cursor.getDouble(index)

    override fun getBoolean(index: Int): Boolean? = cursor.getBoolean(index)
  }

  internal class SqlPreparedStatementAdapter(private val statement: SqlPreparedStatement) : AsyncSqlPreparedStatement {
    override fun bindBytes(index: Int, bytes: ByteArray?) {
      statement.bindBytes(index, bytes)
    }

    override fun bindLong(index: Int, long: Long?) {
      statement.bindLong(index, long)
    }

    override fun bindDouble(index: Int, double: Double?) {
      statement.bindDouble(index, double)
    }

    override fun bindString(index: Int, string: String?) {
      statement.bindString(index, string)
    }

    override fun bindBoolean(index: Int, boolean: Boolean?) {
      statement.bindBoolean(index, boolean)
    }
  }
}
