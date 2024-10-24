package app.cash.sqldelight.driver.r2dbc

import app.cash.sqldelight.db.SqlDriver
import io.r2dbc.spi.Connection

/**
 * The native R2dbcDriver Postgresql driver implements the Postgresql wire protocol for bind arguments -
 * that uses positional arguments and index bind parameters - a special implementation is provided here.
 * Other native R2dbcDrivers for MySql/MariaDb use `?` and is the default in SqlDelight
 */
class PostgresqlR2dbcDriver(connection: Connection) : SqlDriver by R2dbcDriver(connection) {
  override fun createArguments(count: Int): String {
    if (count == 0) return "()"
    return buildString(count * 2 + 1) {
      append("($1")
      repeat(count - 1) { index ->
        append(",$${index + 2}")
      }
      append(')')
    }
  }
}
