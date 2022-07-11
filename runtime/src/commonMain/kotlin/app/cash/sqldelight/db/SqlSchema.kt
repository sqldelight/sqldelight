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
 * API for creating and migrating a SQL database.
 */
interface SqlSchema {
  /**
   * The version of this schema.
   */
  val version: Int

  /**
   * Use [driver] to create the schema from scratch. Assumes no existing database state.
   */
  fun create(driver: SqlDriver): QueryResult<Unit>

  /**
   * Use [driver] to migrate from schema [oldVersion] to [newVersion].
   */
  fun migrate(driver: SqlDriver, oldVersion: Int, newVersion: Int): QueryResult<Unit>
}

/**
 * Represents a block of code [block] that should be executed during a migration after the migration
 * has finished migrating to [afterVersion].
 */
class AfterVersion(
  internal val afterVersion: Int,
  internal val block: (SqlDriver) -> Unit,
)

/**
 * Run [SqlSchema.migrate] normally but execute [callbacks] during the migration whenever
 * it finished upgrading to a version specified by [AfterVersion.afterVersion].
 */
fun SqlSchema.migrateWithCallbacks(
  driver: SqlDriver,
  oldVersion: Int,
  newVersion: Int,
  vararg callbacks: AfterVersion,
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
