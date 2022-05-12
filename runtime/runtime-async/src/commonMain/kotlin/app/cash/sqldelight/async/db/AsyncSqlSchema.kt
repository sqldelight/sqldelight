package app.cash.sqldelight.async.db

/**
 * API for creating and migrating a SQL database.
 */
interface AsyncSqlSchema {
  /**
   * The version of this schema.
   */
  val version: Int

  /**
   * Use [driver] to create the schema from scratch. Assumes no existing database state.
   */
  suspend fun create(driver: AsyncSqlDriver)

  /**
   * Use [driver] to migrate from schema [oldVersion] to [newVersion].
   */
  suspend fun migrate(driver: AsyncSqlDriver, oldVersion: Int, newVersion: Int)
}
