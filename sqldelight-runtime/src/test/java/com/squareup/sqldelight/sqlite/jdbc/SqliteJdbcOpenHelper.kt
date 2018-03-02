package com.squareup.sqldelight.sqlite.jdbc

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class SqliteJdbcOpenHelper : SqlDatabase {
  private val connection = DriverManager.getConnection("jdbc:sqlite:")

  override fun getConnection(): SqlDatabaseConnection = SqliteJdbcConnection(connection)
  override fun close() = connection.close()
}

private class SqliteJdbcConnection(
  private val sqliteConnection: Connection
) : SqlDatabaseConnection {
  override fun prepareStatement(sql: String): SqliteJdbcPreparedStatement {
    return SqliteJdbcPreparedStatement(sqliteConnection.prepareStatement(sql))
  }

  override fun beginTransaction() {
    sqliteConnection.prepareStatement("BEGIN TRANSACTION").execute()
  }

  override fun commitTransaction() {
    sqliteConnection.prepareStatement("END TRANSACTION").execute()
  }

  override fun rollbackTransaction() {
    sqliteConnection.prepareStatement("ROLLBACK TRANSACTION").execute()
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

  override fun bindFloat(index: Int, float: Float?) {
    if (float == null) {
      preparedStatement.setNull(index, Types.REAL)
    } else {
      preparedStatement.setFloat(index, float)
    }
  }

  override fun bindString(index: Int, string: String?) {
    if (string == null) {
      preparedStatement.setNull(index, Types.VARCHAR)
    } else {
      preparedStatement.setString(index, string)
    }
  }

  override fun executeQuery() = SqliteJdbcResultSet(preparedStatement.executeQuery())
  override fun execute() = preparedStatement.executeUpdate()
}

private class SqliteJdbcResultSet(
  private val resultSet: ResultSet
) : SqlResultSet {
  override fun getString(index: Int) = resultSet.getString(index + 1)
  override fun getLong(index: Int) = resultSet.getLong(index + 1)
  override fun getBytes(index: Int) = resultSet.getBytes(index + 1)
  override fun getDouble(index: Int) = resultSet.getDouble(index + 1)
  override fun close() = resultSet.close()
  override fun next() = resultSet.next()
}