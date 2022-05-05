package app.cash.sqldelight.driver.r2dbc

import app.cash.sqldelight.async.AsyncQuery
import app.cash.sqldelight.async.AsyncTransacter
import app.cash.sqldelight.async.db.AsyncSqlCursor
import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.async.db.AsyncSqlPreparedStatement
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle

class R2dbcDriver(private val connection: Connection) : AsyncSqlDriver {
  override suspend fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (AsyncSqlCursor) -> R,
    parameters: Int,
    binders: (AsyncSqlPreparedStatement.() -> Unit)?
  ): R {
    val prepared = connection.createStatement(sql).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }
    val result = prepared.execute().awaitSingle()

    val rowSet = mutableListOf<Map<Int, Any?>>()
    result.map { row, rowMetadata ->
      rowSet.add(rowMetadata.columnMetadatas.mapIndexed { index, _ -> index to row.get(index) }.toMap())
    }.awaitLast()

    return mapper(R2dbcCursor(rowSet))
  }

  override suspend fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (AsyncSqlPreparedStatement.() -> Unit)?
  ): Long {
    val prepared = connection.createStatement(sql).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }

    val result = prepared.execute().awaitSingle()
    return result.rowsUpdated.awaitSingle()
  }

  private val transactions = ThreadLocal<Transaction>()
  private var transaction: Transaction?
    get() = transactions.get()
    set(value) {
      transactions.set(value)
    }

  override suspend fun newTransaction(): AsyncTransacter.Transaction {
    val enclosing = transaction
    val transaction = Transaction(enclosing, connection)
    connection.beginTransaction().awaitSingle()

    return transaction
  }

  override fun currentTransaction(): AsyncTransacter.Transaction? = transaction

  override fun addListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) = Unit
  override fun removeListener(listener: AsyncQuery.Listener, queryKeys: Array<String>) = Unit
  override fun notifyListeners(queryKeys: Array<String>) = Unit

  override suspend fun close() {
    connection.close().awaitSingle()
  }

  private class Transaction(
    override val enclosingTransaction: AsyncTransacter.Transaction?,
    private val connection: Connection
  ) : AsyncTransacter.Transaction() {
    override suspend fun endTransaction(successful: Boolean) {
      if (enclosingTransaction == null) {
        if (successful) connection.commitTransaction().awaitSingle()
        else connection.rollbackTransaction().awaitSingle()
      }
    }
  }
}

open class R2dbcPreparedStatement(private val statement: Statement) : AsyncSqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    if (bytes == null) {
      statement.bindNull(index - 1, ByteArray::class.java)
    } else {
      statement.bind(index - 1, bytes)
    }
  }

  override fun bindLong(index: Int, long: Long?) {
    if (long == null) {
      statement.bindNull(index - 1, Long::class.java)
    } else {
      statement.bind(index - 1, long)
    }
  }

  override fun bindDouble(index: Int, double: Double?) {
    if (double == null) {
      statement.bindNull(index - 1, Double::class.java)
    } else {
      statement.bind(index - 1, double)
    }
  }

  override fun bindString(index: Int, string: String?) {
    if (string == null) {
      statement.bindNull(index - 1, String::class.java)
    } else {
      statement.bind(index - 1, string)
    }
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    if (boolean == null) {
      statement.bindNull(index - 1, Boolean::class.java)
    } else {
      statement.bind(index - 1, boolean)
    }
  }

  fun bindObject(index: Int, any: Any?) {
    if (any == null) {
      statement.bindNull(index - 1, Any::class.java)
    } else {
      statement.bind(index - 1, any)
    }
  }
}

/**
 * TODO: Write a better async cursor API
 */
open class R2dbcCursor(val rowSet: List<Map<Int, Any?>>) : AsyncSqlCursor {
  var row = -1
    private set

  override fun next(): Boolean = ++row < rowSet.size

  override fun getString(index: Int): String? = rowSet[row][index] as String?

  override fun getLong(index: Int): Long? = (rowSet[row][index] as Number?)?.toLong()

  override fun getBytes(index: Int): ByteArray? = rowSet[row][index] as ByteArray?

  override fun getDouble(index: Int): Double? = rowSet[row][index] as Double?

  override fun getBoolean(index: Int): Boolean? = rowSet[row][index] as Boolean?

  inline fun <reified T : Any> getObject(index: Int): T? = rowSet[row][index] as T?

  @Suppress("UNCHECKED_CAST")
  fun <T> getArray(index: Int): Array<T>? = rowSet[row][index] as Array<T>?
}
