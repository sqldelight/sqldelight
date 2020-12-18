package com.squareup.sqldelight.sqlite.driver

import com.squareup.sqldelight.Transacter
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import kotlin.DeprecationLevel.ERROR

class JdbcSqliteDriver constructor(
  /**
   * Database connection URL in the form of `jdbc:sqlite:path` where `path` is either blank
   * (creating an in-memory database) or a path to a file.
   */
  url: String,
  properties: Properties = Properties()
) : JdbcDriver() {
  companion object {
    const val IN_MEMORY = "jdbc:sqlite:"
  }

  @Deprecated(
    "Specify connection URL explicitly",
    ReplaceWith("JdbcSqliteDriver(IN_MEMORY, properties)"), ERROR
  )
  constructor(properties: Properties = Properties()) : this(IN_MEMORY, properties)

  // SQLite only uses a single connection.
  private val connection = DriverManager.getConnection(url, properties)

  private val transactions = ThreadLocal<Transaction>()

  override fun closeConnection(connection: Connection) {
    // No-op
  }

  override fun getConnection() = connection

  override fun close() {
    connection.close()
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

  override fun currentTransaction(): Transacter.Transaction? = transactions.get()

  private inner class Transaction(
    override val enclosingTransaction: Transaction?
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
