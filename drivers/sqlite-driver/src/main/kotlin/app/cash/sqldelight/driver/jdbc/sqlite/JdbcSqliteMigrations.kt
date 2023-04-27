package app.cash.sqldelight.driver.jdbc.sqlite

import app.cash.sqldelight.db.*

private fun JdbcSqliteDriver.getVersion(): Int {
    val mapper = { cursor: SqlCursor ->
        QueryResult.Value(if (cursor.next().value) cursor.getLong(0)?.toInt() else null)
    }

    return executeQuery(null, "PRAGMA user_version", mapper, 0, null).value ?: 0
}

private fun JdbcSqliteDriver.setVersion(version: Int) {
    execute(null, "PRAGMA user_version = $version", 0, null).value
}

/**
 * Uses this driver to create or migrate schema from current `PRAGMA user_version`.
 * Sets new `PRAGMA user_version` to migrated [schema] version
 * Each of the [callbacks] are executed during the migration whenever the upgrade to the version specified by
 * [AfterVersion.afterVersion] has been completed.
 */
fun JdbcSqliteDriver.setupSchema(schema: SqlSchema<QueryResult.Value<Unit>>, vararg callbacks: AfterVersion) {
    val version = getVersion()

    if (version == 0) {
        schema.create(this).value
    } else {
        schema.migrate(this, version, schema.version, *callbacks).value
    }

    setVersion(schema.version)
}
