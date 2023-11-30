import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import java.sql.Connection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Test
import schema.FooQueries

class Testing {
  val fakeDriver = object : JdbcDriver() {
    override fun getConnection() = TODO()
    override fun closeConnection(connection: Connection) = Unit
    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun notifyListeners(vararg queryKeys: String) = Unit
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
