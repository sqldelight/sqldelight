import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.gradle.DriverInitializer

class DriverInitializerImpl : DriverInitializer {
  override fun execute(properties: SqlDelightDatabaseProperties) {
    println("DriverInitializerImpl executed!")
  }
}
