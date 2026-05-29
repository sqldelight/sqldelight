package com.example.sqldelight

import app.cash.sqldelight.db.SqlDriver

class DatabaseFactory(
  private val driver: SqlDriver,
) {
  fun create(): Database = Database(driver)
}
