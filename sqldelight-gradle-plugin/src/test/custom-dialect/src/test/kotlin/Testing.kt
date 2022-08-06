import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import org.junit.Test
import schema.FooQueries
import java.sql.Connection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Testing {
  @Test fun inferredCompiles() {
    val fakeDriver = object : JdbcDriver() {
      override fun getConnection() = TODO()
      override fun closeConnection(connection: Connection) = Unit
      override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
      override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
      override fun notifyListeners(queryKeys: Array<String>) = Unit
    }
    FooQueries(fakeDriver).inferredType(1.seconds)
  }

  @Test fun customFunctionReturnsDuration() {
    val fakeDriver = object : JdbcDriver() {
      override fun getConnection() = TODO()
      override fun closeConnection(connection: Connection) = Unit
      override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
      override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
      override fun notifyListeners(queryKeys: Array<String>) = Unit
    }
    val unused: Duration = FooQueries(fakeDriver).selectFooWithId().executeAsOne()
  }
}
