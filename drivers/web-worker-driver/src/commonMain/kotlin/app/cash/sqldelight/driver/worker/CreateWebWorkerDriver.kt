package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

expect fun createDefaultWebWorkerDriver(): SqlDriver

/**
 * Creates the default SQL.js-backed web worker driver and initializes [schema] before returning.
 *
 * Schema creation or migration and the `PRAGMA user_version` update are performed in the same
 * transaction. Each of [callbacks] runs after its corresponding migration version is complete.
 */
suspend fun createDefaultWebWorkerDriver(
  schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
  migrateEmptySchema: Boolean = false,
  vararg callbacks: AfterVersion,
): SqlDriver {
  val driver = createDefaultWebWorkerDriver()
  return try {
    initializeSchema(driver, schema, migrateEmptySchema, callbacks)
    driver
  } catch (throwable: Throwable) {
    driver.close()
    throw throwable
  }
}

internal suspend fun initializeSchema(
  driver: SqlDriver,
  schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
  migrateEmptySchema: Boolean,
  callbacks: Array<out AfterVersion>,
) {
  val transacter = object : SuspendingTransacterImpl(driver) {}
  transacter.transaction {
    val version = driver.userVersion()

    if (version == 0L && !migrateEmptySchema) {
      schema.create(driver).await()
      driver.setUserVersion(schema.version)
    } else if (version < schema.version) {
      schema.migrate(driver, version, schema.version, *callbacks).await()
      driver.setUserVersion(schema.version)
    }
  }
}

private suspend fun SqlDriver.userVersion(): Long {
  val mapper = { cursor: SqlCursor ->
    QueryResult.AsyncValue {
      if (cursor.next().await()) cursor.getLong(0) else null
    }
  }
  return executeQuery(null, "PRAGMA user_version", mapper, 0, null).await() ?: 0L
}

private suspend fun SqlDriver.setUserVersion(version: Long) {
  execute(null, "PRAGMA user_version = $version", 0, null).await()
}
