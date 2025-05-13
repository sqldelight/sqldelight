package com.squareup.sqldelight.driver.sqlite

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.squareup.sqldelight.driver.test.EphemeralTest

class SqliteEphemeralTest : EphemeralTest() {
  override fun setupDatabase(type: Type): SqlDriver {
    val suffix = when (type) {
      Type.IN_MEMORY -> ":memory:"
      Type.NAMED -> "file:memdb1?mode=memory&cache=shared"
      Type.TEMPORARY -> ""
    }

    return JdbcSqliteDriver("jdbc:sqlite:$suffix", schema = schema)
  }
}
