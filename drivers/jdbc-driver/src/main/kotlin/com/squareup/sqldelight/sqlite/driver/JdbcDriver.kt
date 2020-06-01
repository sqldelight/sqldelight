@file:JvmName("JdbcDrivers")
package com.squareup.sqldelight.sqlite.driver

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
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
}

abstract class JdbcDriver : SqlDriver {
  abstract fun getConnection(): Connection

  private val transactions = ThreadLocal<Transaction>()

  override fun close() = getConnection().close()

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ) {
    getConnection().prepareStatement(sql).use { jdbcStatement ->
      SqliteJdbcPreparedStatement(jdbcStatement)
          .apply { if (binders != null) this.binders() }
          .execute()
    }
  }

  override fun executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): SqlCursor {
    return SqliteJdbcPreparedStatement(getConnection().prepareStatement(sql))
        .apply { if (binders != null) this.binders() }
        .executeQuery()
  }

  override fun newTransaction(): Transacter.Transaction {
    val enclosing = transactions.get()
    val connection = enclosing?.connection ?: getConnection()
    val transaction = Transaction(enclosing, connection)
    transactions.set(transaction)

    if (enclosing == null) {
      connection.autoCommit = false
    }

    return transaction
  }

  override fun currentTransaction(): Transacter.Transaction? = transactions.get()

  private inner class Transaction(
    override val enclosingTransaction: Transaction?,
    internal val connection: Connection
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean) {
      if (enclosingTransaction == null) {
        if (successful) {
          connection.commit()
        } else {
          connection.rollback()
        }
        connection.autoCommit = true
      }
      transactions.set(enclosingTransaction)
    }
  }
}

private class SqliteJdbcPreparedStatement(
  private val preparedStatement: PreparedStatement
) : SqlPreparedStatement {
  override fun bindBytes(index: Int, value: ByteArray?) {
    if (value == null) {
      preparedStatement.setNull(index, Types.BLOB)
    } else {
      preparedStatement.setBytes(index, value)
    }
  }

  override fun bindLong(index: Int, value: Long?) {
    if (value == null) {
      preparedStatement.setNull(index, Types.INTEGER)
    } else {
      preparedStatement.setLong(index, value)
    }
  }

  override fun bindDouble(index: Int, value: Double?) {
    if (value == null) {
      preparedStatement.setNull(index, Types.REAL)
    } else {
      preparedStatement.setDouble(index, value)
    }
  }

  override fun bindString(index: Int, value: String?) {
    if (value == null) {
      preparedStatement.setNull(index, Types.VARCHAR)
    } else {
      preparedStatement.setString(index, value)
    }
  }

  internal fun executeQuery() =
      SqliteJdbcCursor(preparedStatement, preparedStatement.executeQuery())

  internal fun execute() {
    preparedStatement.execute()
  }
}

private class SqliteJdbcCursor(
  private val preparedStatement: PreparedStatement,
  private val resultSet: ResultSet
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
  }
  override fun next() = resultSet.next()
}
