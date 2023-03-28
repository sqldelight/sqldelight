package app.sqltest.shared.common.data

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.sqltest.shared.SqlTestDb

actual class SqlDriverFactoryProvider(private val androidContext: Context) {
  actual fun getDriverFactory(name: String): DriverFactory {
    return object : DriverFactory {
      override suspend fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(SqlTestDb.Schema.synchronous(), androidContext, name)
      }
      override fun isAsync() = false
    }
  }
}
