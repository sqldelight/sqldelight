package com.squareup.sqldelight.sqlite.driver

import com.squareup.sqldelight.sqlite.driver.ConnectionManager.Transaction
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import kotlin.concurrent.getOrSet

class JdbcSqliteDriver constructor(
  /**
   * Database connection URL in the form of `jdbc:sqlite:path` where `path` is either blank
   * (creating an in-memory database) or a path to a file.
   */
  url: String,
  properties: Properties = Properties()
) : JdbcDriver(), ConnectionManager by connectionManager(url, properties) {
  companion object {
    const val IN_MEMORY = "jdbc:sqlite:"
  }
}

private fun connectionManager(url: String, properties: Properties) = when (url) {
  IN_MEMORY -> InMemoryConnectionManager(properties)
  else -> ThreadedConnectionManager(url, properties)
}

private abstract class JdbcSqliteDriverConnectionManager : ConnectionManager {
  override fun Connection.beginTransaction() {
    prepareStatement("BEGIN TRANSACTION").execute()
  }

  override fun Connection.endTransaction() {
    prepareStatement("END TRANSACTION").execute()
  }

  override fun Connection.rollbackTransaction() {
    prepareStatement("ROLLBACK TRANSACTION").execute()
  }
}

private class InMemoryConnectionManager(
  properties: Properties
) : JdbcSqliteDriverConnectionManager() {
  override var transaction: Transaction? = null
  private val connection = DriverManager.getConnection(IN_MEMORY, properties)

  override fun getConnection() = connection
  override fun closeConnection(connection: Connection) = Unit
  override fun close() = connection.close()
}

private class ThreadedConnectionManager(
  private val url: String,
  private val properties: Properties,
) : JdbcSqliteDriverConnectionManager() {
  private val transactions = ThreadLocal<Transaction>()
  private val connections = ThreadLocal<Connection>()

  override var transaction: Transaction?
    get() = transactions.get()
    set(value) { transactions.set(value) }

  override fun getConnection() = connections.getOrSet {
    DriverManager.getConnection(url, properties)
  }

  override fun closeConnection(connection: Connection) {
    check(connections.get() == connection) { "Connections must be closed on the thread that opened them" }
    if (transaction == null) {
      connection.close()
      connections.remove()
    }
  }

  override fun close() = Unit
}
