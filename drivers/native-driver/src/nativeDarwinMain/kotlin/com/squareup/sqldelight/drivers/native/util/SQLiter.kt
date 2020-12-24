package com.squareup.sqldelight.drivers.native.util

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.createDatabaseManager as sqliterCreateDatabaseManager

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal actual fun createDatabaseManager(configuration: DatabaseConfiguration): DatabaseManager {
  return sqliterCreateDatabaseManager(configuration)
}
