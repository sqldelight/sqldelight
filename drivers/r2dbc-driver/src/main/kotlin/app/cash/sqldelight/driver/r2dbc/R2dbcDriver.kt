package app.cash.sqldelight.driver.r2dbc

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle

class R2dbcDriver(private val connection: Connection, private val replaceParameter: Boolean) : SqlDriver {
  private fun calcParameterReplacementAdditionalLength(parameters: Int): Int {
    var numbers = 9 // numbers with current length
    var strLen = 1 // length of each number in characters
    var remaining = parameters // numbers not included in sum
    var lengthSum = 0
    while (remaining > numbers) {
      remaining -= numbers
      lengthSum += strLen * numbers
      numbers *= 10
      strLen += 1
    }
    return lengthSum + remaining * strLen
  }

  private fun replaceParameters(sql: String, parameterIndices: List<Int>): String = if (replaceParameter) {
    val additionalSpace = calcParameterReplacementAdditionalLength(parameterIndices.size)
    buildString(sql.length + additionalSpace) {
      var lastIndex = 0
      parameterIndices.forEachIndexed { parameterIndex, stringIndex ->
        append(sql.substring(lastIndex, stringIndex))
        append("$")
        append(parameterIndex + 1)
        lastIndex = stringIndex + 1
      }
      if (lastIndex < sql.length) {
        append(sql.substring(lastIndex))
      }
    }
  } else {
    sql
  }

  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> R,
    parameterIndices: List<Int>,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<R> {
    val prepared = connection.createStatement(replaceParameters(sql, parameterIndices)).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }

    return QueryResult.AsyncValue {
      val result = prepared.execute().awaitSingle()

      val rowSet = result.map { row, rowMetadata ->
        List(rowMetadata.columnMetadatas.size) { index -> row.get(index) }
      }.asFlow().toList()

      return@AsyncValue mapper(R2dbcCursor(rowSet))
    }
  }

  override fun execute(
    identifier: Int?,
    sql: String,
    parameterIndices: List<Int>,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<Long> {
    val prepared = connection.createStatement(replaceParameters(sql, parameterIndices)).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }

    return QueryResult.AsyncValue {
      val result = prepared.execute().awaitSingle()
      return@AsyncValue result.rowsUpdated.awaitFirstOrNull()?.toLong() ?: 0
    }
  }

  private val transactions = ThreadLocal<Transacter.Transaction>()
  private var transaction: Transacter.Transaction?
    get() = transactions.get()
    set(value) {
      transactions.set(value)
    }

  override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.AsyncValue {
    val enclosing = transaction
    val transaction = Transaction(enclosing, connection)
    this.transaction = transaction

    if (enclosing == null) {
      connection.beginTransaction().awaitFirstOrNull()
    }

    return@AsyncValue transaction
  }

  override fun currentTransaction(): Transacter.Transaction? = transaction

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
  override fun notifyListeners(queryKeys: Array<String>) = Unit

  override fun close() {
    // TODO: Somehow await this async operation
    connection.close()
  }

  private inner class Transaction(
    override val enclosingTransaction: Transacter.Transaction?,
    private val connection: Connection,
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean): QueryResult<Unit> = QueryResult.AsyncValue {
      if (enclosingTransaction == null) {
        if (successful) {
          connection.commitTransaction().awaitFirstOrNull()
        } else {
          connection.rollbackTransaction().awaitFirstOrNull()
        }
      }
      transaction = enclosingTransaction
    }
  }
}

class R2dbcPreparedStatement(private val statement: Statement) : SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    if (bytes == null) {
      statement.bindNull(index, ByteArray::class.java)
    } else {
      statement.bind(index, bytes)
    }
  }

  override fun bindLong(index: Int, long: Long?) {
    if (long == null) {
      statement.bindNull(index, Long::class.java)
    } else {
      statement.bind(index, long)
    }
  }

  override fun bindDouble(index: Int, double: Double?) {
    if (double == null) {
      statement.bindNull(index, Double::class.java)
    } else {
      statement.bind(index, double)
    }
  }

  override fun bindString(index: Int, string: String?) {
    if (string == null) {
      statement.bindNull(index, String::class.java)
    } else {
      statement.bind(index, string)
    }
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    if (boolean == null) {
      statement.bindNull(index, Boolean::class.java)
    } else {
      statement.bind(index, boolean)
    }
  }

  fun bindObject(index: Int, any: Any?) {
    if (any == null) {
      statement.bindNull(index, Any::class.java)
    } else {
      statement.bind(index, any)
    }
  }
}

/**
 * TODO: Write a better async cursor API
 */
class R2dbcCursor(val rowSet: List<List<Any?>>) : SqlCursor {
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
