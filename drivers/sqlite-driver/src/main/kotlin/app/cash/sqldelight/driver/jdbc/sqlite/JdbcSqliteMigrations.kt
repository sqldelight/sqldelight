package app.cash.sqldelight.driver.jdbc.sqlite

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.db.migrateWithCallbacks

fun JdbcSqliteDriver.getVersion(): Int {
  return executeQuery(null, "PRAGMA user_version", { cursor -> if (cursor.next()) cursor.getLong(0)?.toInt() else null }, 0, null).value ?: 0
}

fun JdbcSqliteDriver.setVersion(version: Int) {
  execute(null, "PRAGMA user_version = $version", 0, null)
}

fun JdbcSqliteDriver.createSchema(schema: SqlSchema) {
  schema.create(this)
  setVersion(schema.version)
}

fun JdbcSqliteDriver.migrateSchema(schema: SqlSchema, vararg callbacks: AfterVersion) {
  schema.migrateWithCallbacks(this, getVersion(), schema.version, *callbacks)
  setVersion(schema.version)
}
