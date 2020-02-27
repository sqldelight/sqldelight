package com.squareup.sqldelight.jdbc.driver

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import javax.sql.DataSource

/**
 * A SqlDelight driver backed by a JDBC [DataSource].
 */
open class JdbcDriver private constructor(
  private val dataSource: DataSource? = null,
  private val connection: Connection? = null
): SqlDriver {
  /**
   * A driver wrapped around a single JDBC [Connection]
   */
  constructor(connection: Connection): this(null, connection)

  /**
   * A driver which will invoke [DataSource.getConnection] for a new transaction, which will be used
   * for any subsequent queries.
   */
  constructor(dataSource: DataSource): this(dataSource, null)

  private val transactions = ThreadLocal<Transaction>()

  private fun getConnection() = (transactions.get()?.connection ?: dataSource?.connection ?: connection)!!

  override fun close() = Unit

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
    val transaction = Transaction(enclosing)
    transactions.set(transaction)

    if (enclosing == null) {
      transaction.connection.prepareStatement("BEGIN TRANSACTION").execute()
    }

    return transaction
  }

  override fun currentTransaction(): Transacter.Transaction = transactions.get()

  private inner class Transaction(
    override val enclosingTransaction: Transaction?
  ) : Transacter.Transaction() {
    internal val connection: Connection = getConnection()

    override fun endTransaction(successful: Boolean) {
      if (enclosingTransaction == null) {
        if (successful) {
          connection.prepareStatement("END TRANSACTION").execute()
        } else {
          connection.prepareStatement("ROLLBACK TRANSACTION").execute()
        }
      }
      transactions.set(enclosingTransaction)
    }
  }
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
