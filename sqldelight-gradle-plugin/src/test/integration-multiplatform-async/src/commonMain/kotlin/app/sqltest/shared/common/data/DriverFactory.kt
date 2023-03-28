package app.sqltest.shared.common.data

import app.cash.sqldelight.db.SqlDriver

interface DriverFactory {
  suspend fun createDriver(): SqlDriver
  fun isAsync(): Boolean
}
