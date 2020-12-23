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
package com.squareup.sqldelight.db

import com.squareup.sqldelight.Transacter

/**
 * Maintains connections to an underlying SQL database and provides APIs for managing transactions
 * and executing SQL statements.
 */
interface SqlDriver : Closeable {
  /**
   * Execute a SQL statement and evaluate its result set using the given block.
   *
   * @param [identifier] An opaque, unique value that can be used to implement any driver-side
   *   caching of prepared statements. If [identifier] is null, a fresh statement is required.
   * @param [sql] The SQL string to be executed.
   * @param [parameters] The number of bindable parameters [sql] contains.
   * @param [binders] A lambda which is called before execution to bind any parameters to the SQL
   *   statement.
   * @param [block] A lambda which is called with the cursor when the statement is executed
   *   successfully. The generic result of the lambda is returned to the caller, as soon as the
   *   mutual exclusion on the database connection ends. The cursor **must not escape** the block
   *   scope.
   */
  fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)? = null,
    block: (SqlCursor) -> R
  ): R

  /**
   * Execute a SQL statement.
   *
   * @param [identifier] An opaque, unique value that can be used to implement any driver-side
   *   caching of prepared statements. If [identifier] is null, a fresh statement is required.
   * @param [sql] The SQL string to be executed.
   * @param [parameters] The number of bindable parameters [sql] contains.
   * @param [binders] A lambda which is called before execution to bind any parameters to the SQL
   *   statement.
   */
  fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)? = null
  )

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
