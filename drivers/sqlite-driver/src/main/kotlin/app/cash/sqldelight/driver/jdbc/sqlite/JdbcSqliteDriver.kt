package app.cash.sqldelight.driver.jdbc.sqlite

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.ConnectionManager
import app.cash.sqldelight.driver.jdbc.ConnectionManager.Transaction
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.Properties
import kotlin.concurrent.getOrSet

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class JdbcSqliteDriver constructor(
  /**
   * Database connection URL in the form of `jdbc:sqlite:path?key1=value1&...` where:
   * - `jdbc:sqlite:` is the prefix which instructs [DriverManager] to open a connection
   *   using the provided [org.sqlite.JDBC] Driver.
   * - `path` is a file path which instructs sqlite *where* it should open the database
   *   connection.
   * - `?key1=value1&...` is an optional query string which instruct sqlite *how* it
   *   should open the connection.
   *
   * Examples:
   * - `jdbc:sqlite:/path/to/myDatabase.db` opens a database connection, writing changes
   *   to the filesystem at the specified `path`.
   * - `jdbc:sqlite:` (i.e. an empty path) will create a temporary database whereby the
   *   temp file is deleted upon connection closure.
   * - `jdbc:sqlite::memory:` will create a purely in-memory database.
   * - `jdbc:sqlite:file:memdb1?mode=memory&cache=shared` will create a named in-memory
   *   database which can be shared across connections until all are closed.
   *
   * [sqlite.org/inmemorydb](https://www.sqlite.org/inmemorydb.html)
   */
  url: String,
  properties: Properties = Properties(),
) : JdbcDriver(), ConnectionManager by connectionManager(url, properties) {
  private val listeners = linkedMapOf<String, MutableSet<Query.Listener>>()

  override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
    synchronized(listeners) {
      queryKeys.forEach {
        listeners.getOrPut(it, { linkedSetOf() }).add(listener)
      }
    }
  }

  override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
    synchronized(listeners) {
      queryKeys.forEach {
        listeners[it]?.remove(listener)
      }
    }
  }

  override fun notifyListeners(vararg queryKeys: String) {
    val listenersToNotify = linkedSetOf<Query.Listener>()
    synchronized(listeners) {
      queryKeys.forEach { listeners[it]?.let(listenersToNotify::addAll) }
    }
    listenersToNotify.forEach(Query.Listener::queryResultsChanged)
  }

  companion object {
    const val IN_MEMORY = "jdbc:sqlite:"
  }
}

private fun connectionManager(url: String, properties: Properties): ConnectionManager {
  val path = url.substringBefore('?').substringAfter("jdbc:sqlite:")

  return when {
    path.isEmpty() ||
      path == ":memory:" ||
      path == "file::memory:" ||
      path.startsWith(":resource:") ||
      url.contains("mode=memory") -> StaticConnectionManager(url, properties)
    else -> ThreadedConnectionManager(url, properties)
  }
}

private abstract class JdbcSqliteDriverConnectionManager : ConnectionManager {
  override fun Connection.beginTransaction() {
    prepareStatement("BEGIN TRANSACTION").use(PreparedStatement::execute)
  }

  override fun Connection.endTransaction() {
    prepareStatement("END TRANSACTION").use(PreparedStatement::execute)
  }

  override fun Connection.rollbackTransaction() {
    prepareStatement("ROLLBACK TRANSACTION").use(PreparedStatement::execute)
  }
}

private class StaticConnectionManager(
  url: String,
  properties: Properties,
) : JdbcSqliteDriverConnectionManager() {
  override var transaction: Transaction? = null
  private val connection: Connection = DriverManager.getConnection(url, properties)

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
    set(value) {
      transactions.set(value)
    }

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
