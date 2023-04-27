package app.cash.sqldelight.driver.jdbc.sqlite

import app.cash.sqldelight.db.*
import java.util.*

/**
 * Constructs [JdbcSqliteDriver] and creates or migrates [schema] from current PRAGMA `user_version`.
 * After that updates PRAGMA `user_version` to migrated [schema] version.
 * Each of the [callbacks] are executed during the migration whenever the upgrade to the version specified by
 * [AfterVersion.afterVersion] has been completed.
 *
 * @see JdbcSqliteDriver
 * @see SqlSchema.create
 * @see SqlSchema.migrate
 */
fun JdbcSqliteDriver(
    url: String,
    properties: Properties = Properties(),
    schema: SqlSchema<QueryResult.Value<Unit>>,
    vararg callbacks: AfterVersion,
): JdbcSqliteDriver {
    val driver = JdbcSqliteDriver(url, properties)
    val version = driver.getVersion()

    if (version == 0) {
        schema.create(driver).value
    } else {
        schema.migrate(driver, version, schema.version, *callbacks).value
    }

    driver.setVersion(schema.version)
    return driver
}

private fun JdbcSqliteDriver.getVersion(): Int {
    val mapper = { cursor: SqlCursor ->
        QueryResult.Value(if (cursor.next().value) cursor.getLong(0)?.toInt() else null)
    }
    return executeQuery(null, "PRAGMA user_version", mapper, 0, null).value ?: 0
}

private fun JdbcSqliteDriver.setVersion(version: Int) {
    execute(null, "PRAGMA user_version = $version", 0, null).value
}
