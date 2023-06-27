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

/**
 * API for creating and migrating a SQL database. The implementation of this interface is generated
 * by SQLDelight.
 *
 * @param T Says whether the generated code for this database is asynchronous or synchronous. Most
 *          implementations of [app.cash.sqldelight.SqlDriver] will require one or the other, which
 *          is enforced by requesting the correlated type of Schema ([QueryResult.Value] for
 *          a synchronous runtime, [QueryResult.AsyncValue] for asynchronous). In a multiplatform
 *          environment where you have some drivers that use an asynchronous runtime, and some that
 *          require synchronous, you can generate an asynchronous runtime and then pass a
 *          synchronous schema to synchronous drivers with [SqlSchema.synchronous()]
 */
interface SqlSchema<T : QueryResult<Unit>> {
  /**
   * The version of this schema.
   */
  val version: Long

  /**
   * Use [driver] to create the schema from scratch. Assumes no existing database state.
   */
  fun create(driver: SqlDriver): T

  /**
   * Use [driver] to migrate from schema [oldVersion] to [newVersion].
   * Each of the [callbacks] are executed during the migration whenever the upgrade to the version specified by
   * [AfterVersion.afterVersion] has been completed.
   */
  fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long, vararg callbacks: AfterVersion): T
}

/**
 * Represents a block of code [block] that should be executed during a migration after the migration
 * has finished migrating to [afterVersion].
 */
class AfterVersion(
  val afterVersion: Long,
  val block: (SqlDriver) -> Unit,
)
