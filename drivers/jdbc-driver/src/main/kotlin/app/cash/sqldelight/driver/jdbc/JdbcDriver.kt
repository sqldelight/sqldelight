@file:JvmName("JdbcDrivers")

package app.cash.sqldelight.driver.jdbc

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.ConnectionManager.Transaction
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import javax.sql.DataSource
import org.intellij.lang.annotations.Language

@JvmName("fromDataSource")
fun DataSource.asJdbcDriver() = object : JdbcDriver() {
  override fun getConnection(): Connection {
    return connection
  }

  override fun closeConnection(connection: Connection) {
    connection.close()
  }

  override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
    // No-op. JDBC Driver is not set up for observing queries by default.
  }

  override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
    // No-op. JDBC Driver is not set up for observing queries by default.
  }

  override fun notifyListeners(vararg queryKeys: String) {
    // No-op. JDBC Driver is not set up for observing queries by default.
  }
}

interface ConnectionManager {
  fun close()

  fun getConnection(): Connection

  fun closeConnection(connection: Connection)

  fun Connection.beginTransaction()

  fun Connection.endTransaction()

  fun Connection.rollbackTransaction()

  var transaction: Transaction?

  class Transaction(
    override val enclosingTransaction: Transaction?,
    private val connectionManager: ConnectionManager,
    val connection: Connection,
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean): QueryResult<Unit> {
      try {
        if (enclosingTransaction == null) {
          if (successful) {
            connectionManager.apply { connection.endTransaction() }
          } else {
            connectionManager.apply { connection.rollbackTransaction() }
          }
        }
        // properly rotate the transaction even if there are uncaught errors
      } finally {
        connectionManager.transaction = enclosingTransaction
      }
      return QueryResult.Unit
    }
  }
}

abstract class JdbcDriver :
  SqlDriver,
  ConnectionManager {
  override fun close() {
  }

  override fun Connection.endTransaction() {
    commit()
    autoCommit = true
    closeConnection(this)
  }

  override fun Connection.rollbackTransaction() {
    rollback()
    autoCommit = true
    closeConnection(this)
  }

  override fun Connection.beginTransaction() {
    check(autoCommit) {
      """
      Expected autoCommit to be true by default. For compatibility with SQLDelight make sure it is
      set to true when returning a connection from [JdbcDriver.getConnection()]
      """.trimIndent()
    }
    autoCommit = false
  }

  private val transactions = ThreadLocal<Transaction>()

  override var transaction: Transaction?
    get() = transactions.get()
    set(value) {
      transactions.set(value)
    }

  /**
   * Returns a [Connection] and handler which closes the connection after the transaction finished.
   */
  fun connectionAndClose(): Pair<Connection, () -> Unit> {
    val enclosing = transaction
    return if (enclosing != null) {
      enclosing.connection to {}
    } else {
      val connection = getConnection()
      return connection to { closeConnection(connection) }
    }
  }

  override fun execute(
    identifier: Int?,
    @Language("SQL") sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<Long> {
    val (connection, onClose) = connectionAndClose()
    try {
      return QueryResult.Value(
        connection.prepareStatement(sql).use { jdbcStatement ->
          JdbcPreparedStatement(jdbcStatement)
            .apply { if (binders != null) this.binders() }
            .execute()
        },
      )
    } finally {
      onClose()
    }
  }

  override fun <R> executeQuery(
    identifier: Int?,
    @Language("SQL") sql: String,
    mapper: (SqlCursor) -> QueryResult<R>,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<R> {
    val (connection, onClose) = connectionAndClose()
    try {
      return JdbcPreparedStatement(connection.prepareStatement(sql))
        .apply { if (binders != null) this.binders() }
        .executeQuery(mapper)
    } finally {
      onClose()
    }
  }

  override fun newTransaction(): QueryResult<Transacter.Transaction> {
    val enclosing = transaction
    val connection = enclosing?.connection ?: getConnection()
    val transaction = Transaction(enclosing, this, connection)
    this.transaction = transaction

    if (enclosing == null) {
      connection.beginTransaction()
    }

    return QueryResult.Value(transaction)
  }

  override fun currentTransaction(): Transacter.Transaction? = transaction
}

/**
 * Binds the parameter to [preparedStatement] by calling [bindString], [bindLong] or similar.
 * After binding, [execute] executes the query without a result, while [executeQuery] returns [JdbcCursor].
 */
class JdbcPreparedStatement(
  private val preparedStatement: PreparedStatement,
) : SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    preparedStatement.setBytes(index + 1, bytes)
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    if (boolean == null) {
      preparedStatement.setNull(index + 1, Types.BOOLEAN)
    } else {
      preparedStatement.setBoolean(index + 1, boolean)
    }
  }

  fun bindByte(index: Int, byte: Byte?) {
    if (byte == null) {
      preparedStatement.setNull(index + 1, Types.TINYINT)
    } else {
      preparedStatement.setByte(index + 1, byte)
    }
  }

  fun bindShort(index: Int, short: Short?) {
    if (short == null) {
      preparedStatement.setNull(index + 1, Types.SMALLINT)
    } else {
      preparedStatement.setShort(index + 1, short)
    }
  }

  fun bindInt(index: Int, int: Int?) {
    if (int == null) {
      preparedStatement.setNull(index + 1, Types.INTEGER)
    } else {
      preparedStatement.setInt(index + 1, int)
    }
  }

  override fun bindLong(index: Int, long: Long?) {
    if (long == null) {
      preparedStatement.setNull(index + 1, Types.BIGINT)
    } else {
      preparedStatement.setLong(index + 1, long)
    }
  }

  fun bindFloat(index: Int, float: Float?) {
    if (float == null) {
      preparedStatement.setNull(index + 1, Types.REAL)
    } else {
      preparedStatement.setFloat(index + 1, float)
    }
  }

  override fun bindDouble(index: Int, double: Double?) {
    if (double == null) {
      preparedStatement.setNull(index + 1, Types.DOUBLE)
    } else {
      preparedStatement.setDouble(index + 1, double)
    }
  }

  fun bindBigDecimal(index: Int, decimal: BigDecimal?) {
    preparedStatement.setBigDecimal(index + 1, decimal)
  }

  fun bindObject(index: Int, obj: Any?) {
    if (obj == null) {
      preparedStatement.setNull(index + 1, Types.OTHER)
    } else {
      preparedStatement.setObject(index + 1, obj)
    }
  }

  fun bindObject(index: Int, obj: Any?, type: Int) {
    if (obj == null) {
      preparedStatement.setNull(index + 1, type)
    } else {
      preparedStatement.setObject(index + 1, obj, type)
    }
  }

  override fun bindString(index: Int, string: String?) {
    preparedStatement.setString(index + 1, string)
  }

  fun bindDate(index: Int, date: java.sql.Date?) {
    preparedStatement.setDate(index, date)
  }

  fun bindTime(index: Int, date: java.sql.Time?) {
    preparedStatement.setTime(index, date)
  }

  fun bindTimestamp(index: Int, timestamp: java.sql.Timestamp?) {
    preparedStatement.setTimestamp(index, timestamp)
  }

  fun <R> executeQuery(mapper: (SqlCursor) -> R): R {
    try {
      return preparedStatement.executeQuery()
        .use { resultSet -> mapper(JdbcCursor(resultSet)) }
    } finally {
      preparedStatement.close()
    }
  }

  fun execute(): Long {
    return if (preparedStatement.execute()) {
      // returned true so this is a result set return type.
      0L
    } else {
      preparedStatement.updateCount.toLong()
    }
  }
}

/**
 * Iterate each row in [resultSet] and map the columns to Kotlin classes by calling [getString], [getLong] etc.
 * Use [next] to retrieve the next row and [close] to close the connection.
 */
class JdbcCursor(val resultSet: ResultSet) : SqlCursor {
  override fun getString(index: Int): String? = resultSet.getString(index + 1)
  override fun getBytes(index: Int): ByteArray? = resultSet.getBytes(index + 1)
  override fun getBoolean(index: Int): Boolean? = getAtIndex(index, resultSet::getBoolean)
  fun getByte(index: Int): Byte? = getAtIndex(index, resultSet::getByte)
  fun getShort(index: Int): Short? = getAtIndex(index, resultSet::getShort)
  fun getInt(index: Int): Int? = getAtIndex(index, resultSet::getInt)
  override fun getLong(index: Int): Long? = getAtIndex(index, resultSet::getLong)
  fun getFloat(index: Int): Float? = getAtIndex(index, resultSet::getFloat)
  override fun getDouble(index: Int): Double? = getAtIndex(index, resultSet::getDouble)
  fun getBigDecimal(index: Int): BigDecimal? = resultSet.getBigDecimal(index + 1)
  inline fun <reified T : Any> getObject(index: Int): T? = resultSet.getObject(index + 1, T::class.java)
  fun getDate(index: Int): java.sql.Date? = resultSet.getDate(index)
  fun getTime(index: Int): java.sql.Time? = resultSet.getTime(index)
  fun getTimestamp(index: Int): java.sql.Timestamp? = resultSet.getTimestamp(index)

  @Suppress("UNCHECKED_CAST")
  fun <T> getArray(index: Int) = getAtIndex(index, resultSet::getArray)?.array as Array<T>?

  private fun <T> getAtIndex(index: Int, converter: (Int) -> T): T? =
    converter(index + 1).takeUnless { resultSet.wasNull() }

  override fun next(): QueryResult.Value<Boolean> = QueryResult.Value(resultSet.next())
}
