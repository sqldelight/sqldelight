/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter

/**
 * Maintains connections to an underlying SQL database and provides APIs for managing transactions
 * and executing SQL statements.
 */
interface SqlDriver : Closeable {
  /**
   * Execute a SQL statement and return a [SqlCursor] that iterates the result set.
   *
   * @param [identifier] An opaque, unique value that can be used to implement any driver-side
   *   caching of prepared statements. If [identifier] is null, a fresh statement is required.
   * @param [sql] The SQL string to be executed.
   * @param [parameters] The number of bindable parameters [sql] contains.
   * @param [binders] A lambda which is called before execution to bind any parameters to the SQL
   *   statement.
   */
  fun executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)? = null
  ): SqlCursor

  /**
   * Execute a SQL statement.
   *
   * @param [identifier] An opaque, unique value that can be used to implement any driver-side
   *   caching of prepared statements. If [identifier] is null, a fresh statement is required.
   * @param [sql] The SQL string to be executed.
   * @param [parameters] The number of bindable parameters [sql] contains.
   * @param [binders] A lambda which is called before execution to bind any parameters to the SQL
   *   statement.
   *
   * @return The number of rows updated for an INSERT/DELETE/UPDATE, or 0 for other SQL statements.
   *
   * NOTE it is up to the specific driver to correctly return the row changes. Notably the SQLJS
   * driver does not do this and you should query changes() manually.
   */
  fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)? = null
  ): Long

  /**
   * Start a new [Transacter.Transaction] on the database.
   *
   * It's up to the implementor how this method behaves for different connection/threading patterns.
   */
  fun newTransaction(): Transacter.Transaction

  /**
   * The currently open [Transacter.Transaction] on the database.
   *
   * It's up to the implementor how this method behaves for different connection/threading patterns.
   */
  fun currentTransaction(): Transacter.Transaction?

  fun addListener(listener: Query.Listener, queryKeys: Array<String>)

  fun removeListener(listener: Query.Listener, queryKeys: Array<String>)

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
    fun create(driver: SqlDriver)

    /**
     * Use [driver] to migrate from schema [oldVersion] to [newVersion].
     */
    fun migrate(driver: SqlDriver, oldVersion: Int, newVersion: Int)
  }
}

/**
 * Represents a block of code [block] that should be executed during a migration after the migration
 * has finished migrating to [afterVersion].
 */
class AfterVersion(
  internal val afterVersion: Int,
  internal val block: () -> Unit
)

/**
 * Represents a block of code [block] that should be executed during a migration after the migration
 * has finished migrating to [afterVersion]. Unlike [AfterVersion], this version's lambda accepts a
 * [SqlDriver] as a parameter to make migrations easier.
 */
class AfterVersionWithDriver(
  internal val afterVersion: Int,
  internal val block: (SqlDriver) -> Unit
)

/**
 * Wrap an [AfterVersion] as an [AfterVersionWithDriver].
 */
fun AfterVersion.toAfterVersionWithDriver() =
  AfterVersionWithDriver(afterVersion) { block() }

/**
 * Run [SqlDriver.Schema.migrate] normally but execute [callbacks] during the migration whenever
 * it finished upgrading to a version specified by [AfterVersion.afterVersion]. This method
 * takes [AfterVersion] callbacks, which receive no parameters when invoked.
 */
fun SqlDriver.Schema.migrateWithCallbacks(
  driver: SqlDriver,
  oldVersion: Int,
  newVersion: Int,
  vararg callbacks: AfterVersion
) {
  val wrappedCallbacks = callbacks.map { it.toAfterVersionWithDriver() }.toTypedArray()
  migrateWithCallbacks(driver, oldVersion, newVersion, *wrappedCallbacks)
}

/**
 * Run [SqlDriver.Schema.migrate] normally but execute [callbacks] during the migration whenever
 * it finished upgrading to a version specified by [AfterVersion.afterVersion]. This method
 * takes [AfterVersionWithDriver] callbacks, which receive a [SqlDriver] parameter when invoked.
 */
fun SqlDriver.Schema.migrateWithCallbacks(
  driver: SqlDriver,
  oldVersion: Int,
  newVersion: Int,
  vararg callbacks: AfterVersionWithDriver
) {
  var lastVersion = oldVersion

  // For each callback within the [oldVersion..newVersion) range, alternate between migrating
  // the schema and invoking each callback.
  callbacks.filter { it.afterVersion in oldVersion until newVersion }
    .sortedBy { it.afterVersion }
    .forEach { callback ->
      migrate(driver, oldVersion = lastVersion, newVersion = callback.afterVersion + 1)
      callback.block(driver)
      lastVersion = callback.afterVersion + 1
    }

  // If there were no callbacks, or the newVersion is higher than the highest callback,
  // complete the migration.
  if (lastVersion < newVersion) {
    migrate(driver, lastVersion, newVersion)
  }
}
