package com.squareup.sqldelight.driver.sqlite

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals

class JdbcSqliteDriverInMemoryTest {
  @Test
  fun inMemoryConstantIsThePureMemoryUrl() {
    assertEquals("jdbc:sqlite::memory:", JdbcSqliteDriver.IN_MEMORY)
  }

  @Test
  fun inMemoryConstantIsNotBackedByAFile() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      // database_list.file is empty for both a pure-memory database and a temporary file.
      // journal_mode is "memory" only for the pure-memory database.
      assertEquals("", mainDatabaseFile(driver))
      assertEquals("memory", pragmaResult(driver, "PRAGMA journal_mode"))
    } finally {
      driver.close()
    }
  }

  @Test
  fun emptyPathUrlIsATemporaryFile() {
    val driver = JdbcSqliteDriver("jdbc:sqlite:")
    try {
      assertEquals("delete", pragmaResult(driver, "PRAGMA journal_mode"))
    } finally {
      driver.close()
    }
  }

  private fun mainDatabaseFile(driver: SqlDriver): String? {
    val mapper = { cursor: SqlCursor ->
      var file: String? = null
      while (cursor.next().value) {
        if (cursor.getString(1) == "main") {
          file = cursor.getString(2)
        }
      }
      QueryResult.Value(file)
    }
    return driver.executeQuery(null, "PRAGMA database_list", mapper, 0, null).value
  }

  private fun pragmaResult(driver: SqlDriver, sql: String): String? {
    val mapper = { cursor: SqlCursor ->
      QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
    }
    return driver.executeQuery(null, sql, mapper, 0, null).value
  }
}
