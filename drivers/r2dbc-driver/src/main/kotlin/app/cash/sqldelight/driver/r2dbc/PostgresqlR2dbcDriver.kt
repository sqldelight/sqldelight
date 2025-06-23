package app.cash.sqldelight.driver.r2dbc

import app.cash.sqldelight.db.SqlDriver
import io.r2dbc.spi.Connection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The native R2dbcDriver Postgresql driver implements the Postgresql wire protocol for bind arguments -
 * that uses positional arguments and index bind parameters - a special implementation is provided here.
 * Other native R2dbcDrivers for MySql/MariaDb use `?` and is the default in SqlDelight
 */
class PostgreSqlR2dbcDriver(val connection: Connection, val closed: (Throwable?) -> Unit = { }) : SqlDriver by R2dbcDriver(connection, closed) {
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

/**
 * Creates and returns a [PostgreSqlR2dbcDriver] with the given [connection].
 *
 * The scope waits until the driver is closed [PostgreSqlR2dbcDriver.close].
 */
fun CoroutineScope.PostgreSqlR2dbcDriver(
  connection: Connection,
): PostgreSqlR2dbcDriver {
  val completed = Job()
  val driver = PostgreSqlR2dbcDriver(connection) {
    if (it == null) {
      completed.complete()
    } else {
      completed.completeExceptionally(it)
    }
  }
  launch {
    completed.join()
  }
  return driver
}
