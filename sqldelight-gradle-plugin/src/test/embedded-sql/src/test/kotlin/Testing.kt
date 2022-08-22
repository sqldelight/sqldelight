import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import org.junit.Test
import schema.FooQueries
import java.sql.Connection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Testing {
  private val fakeDriver = object : JdbcDriver() {
    override fun getConnection() = TODO()
    override fun closeConnection(connection: Connection) = Unit
    override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
    override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
    override fun notifyListeners(queryKeys: Array<String>) = Unit
  }

  @Test fun customFunctionReturnsDuration() {
    val unused: Duration = FooQueries(fakeDriver).selectFooWithId().executeAsOne()
  }

  @Test fun inferredCompiles() {
    FooQueries(fakeDriver).inferredType(1.seconds)
  }

  @Test fun inferredTypeFromMaxIsLong() {
    FooQueries(fakeDriver).inferredTypeFromMax(1L).executeAsOne()
  }
}
