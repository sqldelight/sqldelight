package com.squareup.sqldelight.sqlite.driver

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlCursor
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class SqliteJdbcOpenHelper(name: String = "jdbc:sqlite:") : SqlDatabase {
  private val connection = SqliteJdbcConnection(DriverManager.getConnection(name))

  override fun getConnection(): SqlDatabaseConnection = connection
  override fun close() = connection.close()
}

private class SqliteJdbcConnection(
  private val sqliteConnection: Connection
) : SqlDatabaseConnection {
  private val transactions = ThreadLocal<Transaction>()

  fun close() = sqliteConnection.close()

  override fun prepareStatement(
    sql: String,
    type: SqlPreparedStatement.Type,
    parameters: Int
  ): SqliteJdbcPreparedStatement {
    return SqliteJdbcPreparedStatement(sqliteConnection.prepareStatement(sql))
  }

  override fun newTransaction(): Transaction {
    val enclosing = transactions.get()
    val transaction = Transaction(enclosing)
    transactions.set(transaction)

    if (enclosing == null) {
      sqliteConnection.prepareStatement("BEGIN TRANSACTION").execute()
    }

    return transaction
  }

  override fun currentTransaction() = transactions.get()

  private inner class Transaction(
    override val enclosingTransaction: Transaction?
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean) {
      if (enclosingTransaction == null) {
        if (successful) {
          sqliteConnection.prepareStatement("END TRANSACTION").execute()
        } else {
          sqliteConnection.prepareStatement("ROLLBACK TRANSACTION").execute()
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

  override fun executeQuery() = SqliteJdbcCursor(preparedStatement.executeQuery())
  override fun execute() = preparedStatement.executeUpdate().toLong()
}

private class SqliteJdbcCursor(
  private val resultSet: ResultSet
) : SqlCursor {
  override fun getString(index: Int) = resultSet.getString(index + 1)
  override fun getBytes(index: Int) = resultSet.getBytes(index + 1)
  override fun getLong(index: Int): Long? {
    val value = resultSet.getLong(index + 1)
    if (resultSet.wasNull()) return null
    return value
  }
  override fun getDouble(index: Int): Double? {
    val value = resultSet.getDouble(index + 1)
    if (resultSet.wasNull()) return null
    return value
  }
  override fun close() = resultSet.close()
  override fun next() = resultSet.next()
}
