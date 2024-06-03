package app.cash.sqldelight.driver.kmp

import androidx.sqlite.SQLiteDriver
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlSchema

/**
 * Constructs [KmpSqliteDriver] and creates or migrates [schema] from current PRAGMA `user_version`.
 * After that updates PRAGMA `user_version` to migrated [schema] version.
 * Each of the [callbacks] are executed during the migration whenever the upgrade to the version specified by
 * [AfterVersion.afterVersion] has been completed.
 *
 * @see KmpSqliteDriver
 * @see SqlSchema.create
 * @see SqlSchema.migrate
 */
fun KmpSqliteDriver(
  driver: SQLiteDriver,
  name: String,
  schema: SqlSchema<QueryResult.Value<Unit>>,
  migrateEmptySchema: Boolean = false,
  cacheSize: Int = DEFAULT_CACHE_SIZE,
  vararg callbacks: AfterVersion,
): KmpSqliteDriver {
  val kmpDriver = KmpSqliteDriver(driver, name, cacheSize)
  val transacter = object : TransacterImpl(kmpDriver) {}

  transacter.transaction {
    val version = kmpDriver.getVersion()

    if (version == 0L && !migrateEmptySchema) {
      schema.create(kmpDriver).value
      kmpDriver.setVersion(schema.version)
    } else if (version < schema.version) {
      schema.migrate(kmpDriver, version, schema.version, *callbacks).value
      kmpDriver.setVersion(schema.version)
    }
  }

  return kmpDriver
}

private fun KmpSqliteDriver.getVersion(): Long {
  val mapper = { cursor: SqlCursor ->
    QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
  }
  return executeQuery(null, "PRAGMA user_version", mapper, 0, null).value ?: 0L
}

private fun KmpSqliteDriver.setVersion(version: Long) {
  execute(null, "PRAGMA user_version = $version", 0, null).value
}
