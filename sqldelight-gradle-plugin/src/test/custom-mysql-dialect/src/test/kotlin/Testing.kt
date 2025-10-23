import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import org.junit.Test
import schema.InetxxxQueries
import java.sql.Connection

class Testing {
  val fakeDriver = object : JdbcDriver() {
    override fun getConnection() = TODO()
    override fun closeConnection(connection: Connection) = Unit
    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun notifyListeners(vararg queryKeys: String) = Unit
  }

  @Test fun convertAddressToNumerical() {
    InetxxxQueries(fakeDriver).insertOneForTestINET_ATON("127.0.0.1")
  }

  @Test fun convertNumericalToAddresses() {
    InetxxxQueries(fakeDriver).selectForTestINET_NTOA().executeAsList()
  }
}
