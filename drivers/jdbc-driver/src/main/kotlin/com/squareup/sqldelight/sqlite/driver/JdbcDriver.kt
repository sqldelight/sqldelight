@file:JvmName("JdbcDrivers")
package com.squareup.sqldelight.sqlite.driver

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.sqlite.driver.ConnectionManager.Transaction
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import javax.sql.DataSource

@JvmName("fromDataSource")
fun DataSource.asJdbcDriver() = object : JdbcDriver() {
  override fun getConnection(): Connection {
    return connection
  }

  override fun closeConnection(connection: Connection) {
    connection.close()
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
    val connection: Connection
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean) {
      if (enclosingTransaction == null) {
        if (successful) connectionManager.apply { connection.endTransaction() }
        else connectionManager.apply { connection.rollbackTransaction() }
      }
      connectionManager.transaction = enclosingTransaction
    }
  }
}

abstract class JdbcDriver : SqlDriver, ConnectionManager {
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
    autoCommit = false
  }

  private val transactions = ThreadLocal<Transaction>()

  override var transaction: Transaction?
    get() = transactions.get()
    set(value) { transactions.set(value) }

  private fun connectionAndClose(): Pair<Connection, () -> Unit> {
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
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ) {
    val (connection, onClose) = connectionAndClose()
    connection.prepareStatement(sql).use { jdbcStatement ->
      SqliteJdbcPreparedStatement(jdbcStatement)
        .apply { if (binders != null) this.binders() }
        .execute()
    }
    onClose()
  }

  override fun executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): SqlCursor {
    val (connection, onClose) = connectionAndClose()
    return SqliteJdbcPreparedStatement(connection.prepareStatement(sql))
      .apply { if (binders != null) this.binders() }
      .executeQuery(onClose)
  }

  override fun newTransaction(): Transacter.Transaction {
    val enclosing = transaction
    val connection = enclosing?.connection ?: getConnection()
    val transaction = Transaction(enclosing, this, connection)
    this.transaction = transaction

    if (enclosing == null) {
      connection.beginTransaction()
    }

    return transaction
  }

  override fun currentTransaction(): Transacter.Transaction? = transaction
}

private class SqliteJdbcPreparedStatement(
  private val preparedStatement: PreparedStatement
) : SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    if (bytes == null) {
      preparedStatement.setNull(index, Types.BLOB)
    } else {
      preparedStatement.setBytes(index, bytes)
    }
  }

  override fun bindLong(index: Int, long: Long?) {
    if (long == null) {
      preparedStatement.setNull(index, Types.INTEGER)
    } else {
      preparedStatement.setLong(index, long)
    }
  }

  override fun bindDouble(index: Int, double: Double?) {
    if (double == null) {
      preparedStatement.setNull(index, Types.REAL)
    } else {
      preparedStatement.setDouble(index, double)
    }
  }

  override fun bindString(index: Int, string: String?) {
    if (string == null) {
      preparedStatement.setNull(index, Types.VARCHAR)
    } else {
      preparedStatement.setString(index, string)
    }
  }

  fun executeQuery(onClose: () -> Unit) =
    SqliteJdbcCursor(preparedStatement, preparedStatement.executeQuery(), onClose)

  fun execute() {
    preparedStatement.execute()
  }
}

private class SqliteJdbcCursor(
  private val preparedStatement: PreparedStatement,
  private val resultSet: ResultSet,
  private val onClose: () -> Unit
) : SqlCursor {
  override fun getString(index: Int) = resultSet.getString(index + 1)
  override fun getBytes(index: Int) = resultSet.getBytes(index + 1)
  override fun getLong(index: Int): Long? {
    return resultSet.getLong(index + 1).takeUnless { resultSet.wasNull() }
  }
  override fun getDouble(index: Int): Double? {
    return resultSet.getDouble(index + 1).takeUnless { resultSet.wasNull() }
  }
  override fun close() {
    resultSet.close()
    preparedStatement.close()
    onClose()
  }
  override fun next() = resultSet.next()
}
