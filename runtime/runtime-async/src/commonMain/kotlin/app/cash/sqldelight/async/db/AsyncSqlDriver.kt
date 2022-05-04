package app.cash.sqldelight.async.db

import app.cash.sqldelight.async.AsyncQuery
import app.cash.sqldelight.async.AsyncTransacter

interface AsyncSqlDriver : Closeable {
  suspend fun <R> executeQuery(
          identifier: Int?,
          sql: String,
          mapper: (AsyncSqlCursor) -> R,
          parameters: Int,
          binders: (AsyncSqlPreparedStatement.() -> Unit)? = null,
  ): R

  suspend fun execute(
          identifier: Int?,
          sql: String,
          parameters: Int,
          binders: (AsyncSqlPreparedStatement.() -> Unit)? = null,
  ): Long

  /**
   * Start a new [AsyncTransacter.Transaction] on the database.
   *
   * It's up to the implementor how this method behaves for different connection/threading patterns.
   */
  suspend fun newTransaction(): AsyncTransacter.Transaction

  /**
   * The currently open [AsyncTransacter.Transaction] on the database.
   *
   * It's up to the implementor how this method behaves for different connection/threading patterns.
   */
  fun currentTransaction(): AsyncTransacter.Transaction?

  fun addListener(listener: AsyncQuery.Listener, queryKeys: Array<String>)

  fun removeListener(listener: AsyncQuery.Listener, queryKeys: Array<String>)

  fun notifyListeners(queryKeys: Array<String>)

  /**
   * API for creating and migrating a SQL database.
   */
  interface Schema {
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
}
