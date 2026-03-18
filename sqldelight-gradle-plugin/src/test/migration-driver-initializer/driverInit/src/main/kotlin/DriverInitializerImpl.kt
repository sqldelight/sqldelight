import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.gradle.DriverInitializer
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.util.Properties
import java.util.logging.Logger
import org.sqlite.JDBC

class DriverInitializerImpl : DriverInitializer {
  override fun execute(properties: SqlDelightDatabaseProperties, driverProperties: Properties) {
    println("DriverInitializerImpl executed!")

    val customDriver = CustomDriver()

    val driverList = DriverManager.getDrivers().toList()
    for (driver in driverList) {
      DriverManager.deregisterDriver(driver)
    }

    DriverManager.registerDriver(customDriver)
  }
}

/**
 * Wrapper around JDBC
 */
class CustomDriver : Driver {

  private val wrappedDriver: JDBC = JDBC()

  override fun acceptsURL(url: String?): Boolean = wrappedDriver.acceptsURL(url)

  override fun connect(url: String?, props: Properties?): Connection {
    // test that we use the registered custom driver to connect to sqlite dbs during gradle tasks
    println("CustomDriver is used for connection.")
    val connection = wrappedDriver.connect(url, props)
    return connection
  }

  override fun getMajorVersion(): Int = wrappedDriver.majorVersion

  override fun getMinorVersion(): Int = wrappedDriver.minorVersion

  override fun getPropertyInfo(url: String?, info: Properties?): Array<DriverPropertyInfo> = wrappedDriver.getPropertyInfo(url, info)

  override fun jdbcCompliant(): Boolean = wrappedDriver.jdbcCompliant()

  override fun getParentLogger(): Logger = wrappedDriver.parentLogger
}
