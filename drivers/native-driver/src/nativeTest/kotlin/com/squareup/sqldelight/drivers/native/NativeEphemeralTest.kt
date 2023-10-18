package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.driver.test.EphemeralTest

class NativeEphemeralTest : EphemeralTest() {

  // TODO: Issue #3241
  override val skipNamed: Boolean = true

  override fun setupDatabase(type: Type): SqlDriver {
    return NativeSqliteDriver(
      schema = schema,
      name = "replaceme",
      onConfiguration = { configuration ->
        configuration.copy(
          name = when (type) {
            Type.IN_MEMORY -> null
            Type.NAMED -> "memdb1"
            Type.TEMPORARY -> ""
          },
          inMemory = when (type) {
            Type.IN_MEMORY -> true
            Type.NAMED -> true
            Type.TEMPORARY -> false
          },
          extendedConfig = DatabaseConfiguration.Extended(
            basePath = when (type) {
              Type.IN_MEMORY -> null
              Type.NAMED -> null
              Type.TEMPORARY -> ""
            },
          ),
        )
      },
    )
  }
}
