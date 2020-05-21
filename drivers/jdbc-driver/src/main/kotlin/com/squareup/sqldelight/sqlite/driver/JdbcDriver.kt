package com.squareup.sqldelight.sqlite.driver

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

abstract class JdbcDriver : SqlDriver {
  abstract fun getConnection(): Connection

  private val transactions = ThreadLocal<Transacter.Transaction>()

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
    val transaction = Transaction(enclosing)
    transactions.set(transaction)

    if (enclosing == null) {
      getConnection().prepareStatement("BEGIN TRANSACTION").execute()
    }

    return transaction
  }

  override fun currentTransaction() = transactions.get()

  private inner class Transaction(
    override val enclosingTransaction: Transacter.Transaction?
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean) {
      if (enclosingTransaction == null) {
        if (successful) {
          getConnection().prepareStatement("END TRANSACTION").execute()
        } else {
          getConnection().prepareStatement("ROLLBACK TRANSACTION").execute()
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
  override fun next() = resultSet.next()
  override fun getColumnCount() =  resultSet.metaData.columnCount
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
}
