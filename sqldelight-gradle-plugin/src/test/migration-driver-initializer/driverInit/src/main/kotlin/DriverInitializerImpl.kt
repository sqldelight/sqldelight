import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.gradle.DriverInitializer
import java.util.Properties

class DriverInitializerImpl : DriverInitializer {
  override fun execute(properties: SqlDelightDatabaseProperties, driverProperties: Properties) {
    println("DriverInitializerImpl executed!")
  }
}
