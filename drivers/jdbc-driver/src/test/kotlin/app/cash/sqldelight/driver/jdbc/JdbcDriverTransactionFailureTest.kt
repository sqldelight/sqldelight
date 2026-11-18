package app.cash.sqldelight.driver.jdbc

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JdbcDriverTransactionFailureTest {

  private val driver = ConnectionTrackingJdbcDriver()
  private val transacter = object : TransacterImpl(driver) {}

  @AfterTest
  fun checkConnectionsClosedAndClear() {
    try {
      assertEquals(0, driver.connectionCount, "connection was left open")
    } finally {
      driver.closeConnections()
      driver.configureNewConnections { }
    }
  }

  @Test
  fun `connection closed and autoCommit not reset on rollback failure`() {
    driver.configureNewConnections {
      beforeRollback = { throw SQLException("rollback failed") }
    }

    val failure = assertFailsWith<SQLException> {
      transacter.transaction { throw SQLException("failure in transaction") }
    }
    assertEquals("failure in transaction", failure.message)
    assertEquals("rollback failed", failure.suppressed.single().message)

    assertEquals(
      listOf("setAutoCommit(false)", "rollback", "close"),
      driver.lastConnection.calls,
    )
  }

  @Test
  fun `connection closed on autoCommit reset failure`() {
    driver.configureNewConnections {
      onAutoCommit = { autoCommit -> if (autoCommit) throw SQLException("reset autoCommit failed") }
    }

    val failure = assertFailsWith<SQLException> { transacter.transaction { } }
    assertEquals("reset autoCommit failed", failure.message)

    assertEquals(
      listOf("setAutoCommit(false)", "commit", "setAutoCommit(true)", "close"),
      driver.lastConnection.calls,
    )
  }

  @Test
  fun `connection closed on beginTransaction failure`() {
    driver.configureNewConnections {
      onAutoCommit = { autoCommit ->
        // autoCommit is set to false in beginTransaction
        if (!autoCommit) throw SQLException("unset autoCommit failed")
      }
    }

    val failure = assertFailsWith<SQLException> { transacter.transaction { } }
    assertEquals("unset autoCommit failed", failure.message)

    assertEquals(
      listOf("setAutoCommit(false)", "close"),
      driver.lastConnection.calls,
    )

    // subsequent transaction succeeds
    driver.configureNewConnections { /* defaults */ }
    transacter.transaction { } // succeeds
    assertEquals(
      listOf("setAutoCommit(false)", "commit", "setAutoCommit(true)", "close"),
      driver.lastConnection.calls,
    )
  }

  @Test
  fun `connection closed and autoCommit not restored when commit and rollback both fail`() {
    driver.configureNewConnections {
      beforeCommit = { throw SQLException("commit failed") }
      beforeRollback = { throw SQLException("rollback failed") }
    }
    val failure = assertFailsWith<SQLException> { transacter.transaction { } }
    assertEquals("commit failed", failure.message)
    assertEquals("rollback failed", failure.suppressed.single().message)

    assertEquals(
      listOf("setAutoCommit(false)", "commit", "rollback", "close"),
      driver.lastConnection.calls,
    )
  }

  @Test
  fun `connection closed and autoCommit restored when commit fails but rollback succeeds`() {
    driver.configureNewConnections {
      beforeCommit = { throw SQLException("commit failed") }
    }

    val failure =
      assertFailsWith<SQLException> { transacter.transaction { } }
    assertEquals("commit failed", failure.message)

    assertEquals(
      listOf("setAutoCommit(false)", "commit", "rollback", "setAutoCommit(true)", "close"),
      driver.lastConnection.calls,
    )
  }
}

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
private class FailableConnection(val delegate: Connection) : Connection by delegate {
  var beforeCommit: () -> Unit = { }
  var beforeRollback: () -> Unit = { }
  var onAutoCommit: (Boolean) -> Unit = { }
  var lastAutoCommit: Boolean? = null
  val calls = mutableListOf<String>()

  override fun commit() {
    calls += "commit"
    beforeCommit()
    delegate.commit()
  }

  override fun rollback() {
    calls += "rollback"
    beforeRollback()
    delegate.rollback()
  }

  override fun setAutoCommit(autoCommit: Boolean) {
    calls += "setAutoCommit($autoCommit)"
    lastAutoCommit = autoCommit
    onAutoCommit(autoCommit)
    delegate.autoCommit = autoCommit
  }

  override fun close() {
    calls += "close"
    delegate.close()
  }
}

private class ConnectionTrackingJdbcDriver : JdbcDriver() {
  private val openConnections: MutableSet<FailableConnection> = Collections.newSetFromMap(IdentityHashMap())
  val connectionCount get() = openConnections.size
  lateinit var lastConnection: FailableConnection
    private set

  private var configure: FailableConnection.() -> Unit = {}

  fun configureNewConnections(configure: FailableConnection.() -> Unit) {
    this.configure = configure
  }

  fun closeConnections() {
    openConnections.forEach { it.close() }
    openConnections.clear()
  }

  override fun getConnection(): Connection {
    val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    val wrapped = FailableConnection(connection)
    wrapped.configure()
    openConnections += wrapped
    lastConnection = wrapped
    return wrapped
  }

  override fun closeConnection(connection: Connection) {
    try {
      connection.close()
    } finally {
      // whether it failed or not - close was attempted
      (connection as? FailableConnection)?.also { openConnections -= it }
    }
  }

  override fun addListener(vararg queryKeys: String, listener: Query.Listener) {}
  override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {}
  override fun notifyListeners(vararg queryKeys: String) {}
}
