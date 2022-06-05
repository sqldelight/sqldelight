package app.cash.sqldelight.driver.r2dbc

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle

class R2dbcDriver(private val connection: Connection) : SqlDriver {
  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> R,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): QueryResult<R> {
    val prepared = connection.createStatement(sql).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }

    return QueryResult.AsyncValue {
      val result = prepared.execute().awaitSingle()

      val rowSet = mutableListOf<Map<Int, Any?>>()
      result.map { row, rowMetadata ->
        rowSet.add(rowMetadata.columnMetadatas.mapIndexed { index, _ -> index to row.get(index) }.toMap())
      }.awaitLast()

      return@AsyncValue mapper(R2dbcCursor(rowSet))
    }
  }

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): QueryResult<Long> {
    val prepared = connection.createStatement(sql).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }

    return QueryResult.AsyncValue {
      val result = prepared.execute().awaitSingle()
      return@AsyncValue result.rowsUpdated.awaitSingle()
    }
  }

  private val transactions = ThreadLocal<Transaction>()
  private var transaction: Transaction?
    get() = transactions.get()
    set(value) {
      transactions.set(value)
    }

  override fun newTransaction(): Transacter.Transaction {
    throw NotImplementedError("Begin transaction manually using execute()")
  }

  override fun currentTransaction(): Transacter.Transaction? = transaction

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
  override fun notifyListeners(queryKeys: Array<String>) = Unit

  override fun close() {
    // TODO: Somehow await this async operation
    connection.close()
  }

  private class Transaction(
    override val enclosingTransaction: Transacter.Transaction?,
    private val connection: Connection
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean) {
      throw NotImplementedError("End transaction manually using execute()")
    }
  }
}

open class R2dbcPreparedStatement(private val statement: Statement) : SqlPreparedStatement {
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
open class R2dbcCursor(val rowSet: List<Map<Int, Any?>>) : SqlCursor {
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
