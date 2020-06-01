package com.squareup.sqldelight.sqlite.driver

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

  @Deprecated("Specify connection URL explicitly",
      ReplaceWith("JdbcSqliteDriver(IN_MEMORY, properties)"), ERROR)
  constructor(properties: Properties = Properties()) : this(IN_MEMORY, properties)

  // SQLite only uses a single connection.
  private val connection = DriverManager.getConnection(url, properties)

  override fun closeConnection(connection: Connection) {
    // No-op
  }

  override fun getConnection() = connection

  override fun close() {
    connection.close()
  }
}
