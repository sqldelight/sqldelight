import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import org.junit.Test
import schema.FooQueries
import schema.SelectWithBinding
import schema.Set
import schema.SetSelect
import java.sql.Connection

class Testing {
  private val fakeDriver = object : JdbcDriver() {
    override fun getConnection() = TODO()
    override fun closeConnection(connection: Connection) = Unit
    override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
    override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
    override fun notifyListeners(queryKeys: Array<String>) = Unit
  }

  @Test fun selectWithBinding() {
    FooQueries(fakeDriver).selectWithBinding(42.0, id = 4) { avg: Double?, id: Long? ->
    }

    val result: SelectWithBinding = FooQueries(fakeDriver).selectWithBinding(42.0, id = 4).executeAsOne()
    val avg: Double? = result.avg
    val id: Long? = result.id
  }

  @Test fun set() {
    val result: Set = FooQueries(fakeDriver).set(42.0).executeAsOne()
    val avg: Double? = result.avg
    val id: Long = result.expr

    FooQueries(fakeDriver).set(42.0) { avg: Double?, expr: Long ->
    }
  }

  @Test fun setSelect() {
    FooQueries(fakeDriver).setSelect(42.0, 4) { avg: Double?, id: Long? ->
    }
    val result: SetSelect = FooQueries(fakeDriver).setSelect(42.0, 4).executeAsOne()
    val avg: Double? = result.avg
    val id: Long? = result.id
  }
}
