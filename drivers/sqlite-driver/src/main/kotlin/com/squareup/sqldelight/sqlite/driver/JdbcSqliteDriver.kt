package com.squareup.sqldelight.sqlite.driver

import com.squareup.sqldelight.jdbc.driver.JdbcDriver
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import kotlin.DeprecationLevel.ERROR


class JdbcSqliteDriver @Deprecated(
    message = "Use JdbcDriver with a connection",
    replaceWith = ReplaceWith(
        expression = "JdbcDriver(connection)",
        imports = arrayOf(
            "com.squareup.sqldelight.jdbc.driver.JdbcDriver",
            "java.sql.DriverManager",
            "java.util.Properties"
        )
    ),
    level = ERROR
) constructor(
  /**
   * Database connection URL in the form of `jdbc:sqlite:path` where `path` is either blank
   * (creating an in-memory database) or a path to a file.
   */
  url: String,
  properties: Properties = Properties(),
  connection: Connection = DriverManager.getConnection(url, properties)
) : JdbcDriver(connection) {
  companion object {
    const val IN_MEMORY = "jdbc:sqlite:"
  }
}