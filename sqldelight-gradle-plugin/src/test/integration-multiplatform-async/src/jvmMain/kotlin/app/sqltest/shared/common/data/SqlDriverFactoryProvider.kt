package app.sqltest.shared.common.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class SqlDriverFactoryProvider() {
  actual fun getDriverFactory(name: String): DriverFactory {
    return object : DriverFactory {
      override suspend fun createDriver(): SqlDriver {
        return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
      }
      override fun isAsync() = false
    }
  }
}
