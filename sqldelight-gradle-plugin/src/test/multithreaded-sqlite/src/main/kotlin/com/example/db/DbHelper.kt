package com.example.db

import com.example.db.Database.Companion.Schema
import com.example.db.Database.Companion.invoke
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.util.Properties

/**
 * Database helper inits driver, creates schema, handles migrations and closes database.
 *
 * @property foreignKeys Corresponds to foreign_keys pragma and is true by default.
 * It turns on after schema creation for new databases, but turns before migration for existent databases.
 *
 * @property journalMode Corresponds to journal_mode pragma, should be set before first access to [database] property.
 */
class DbHelper(
  journalMode: String = "",
  foreignKeys: Boolean = false
) {
  val database: Database by lazy {
    if (journalMode.isNotEmpty()) {
      this.journalMode = journalMode
    }
    val currentVer = version
    if (currentVer == 0) {
      Schema.create(driver)
      version = Schema.version
      this.foreignKeys = foreignKeys
    } else {
      this.foreignKeys = foreignKeys
      val schemaVer = Schema.version
      if (schemaVer > currentVer) {
        Schema.migrate(driver, currentVer, schemaVer)
        version = schemaVer
        this.foreignKeys = foreignKeys
      }
    }
    invoke(driver)
  }

  fun close() {
    driver.close()
  }

  private val driver: JdbcSqliteDriver by lazy {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + "test.db", Properties())
  }

  private var version: Int
    get() {
      val sqlCursor = driver.executeQuery(null, "PRAGMA user_version;", 0, null)
      return sqlCursor.getLong(0)!!.toInt()
    }
    set(value) {
      driver.execute(null, "PRAGMA user_version = $value;", 0, null)
    }

  var journalMode: String = journalMode
    private set(value) {
      driver.execute(null, "PRAGMA journal_mode = $value;", 0, null)
      field = value
    }

  var foreignKeys: Boolean = foreignKeys
    set(value) {
      driver.execute(null, "PRAGMA foreign_keys = ${if (value) 1 else 0};", 0, null)
      field = value
    }
}
