/*
 * Copyright (C) 2026 Block, Inc.
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
package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.api.WorkerAction
import app.cash.sqldelight.driver.worker.api.WorkerActions
import app.cash.sqldelight.driver.worker.api.WorkerWrapperRequest
import app.cash.sqldelight.driver.worker.expected.Worker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Configuration for the SQLite Wasm OPFS worker.
 *
 * @property databaseName database file name, relative to the origin-private file system root.
 */
data class SqliteWasmWorkerConfig(
  val databaseName: String = "sqldelight.db",
)

/**
 * A SQLite Wasm OPFS [SqlDriver] backed by an ECMAScript module Web Worker.
 *
 * Instances are created by [createSqliteWasmWebWorkerDriver]. Call [closeAndAwait] when
 * deterministic shutdown is required: it waits for SQLite to close the database before releasing
 * the Worker. The synchronous [close] method immediately releases the Worker and cannot await that
 * acknowledgement. [deleteDatabase] deletes the configured database after closing it, then
 * releases the Worker. All lifecycle methods are idempotent.
 */
class SqliteWasmWebWorkerDriver internal constructor(
  private val driver: WebWorkerDriver,
  private val lifecycleWrapper: WorkerWrapper,
  private var lifecycleRequestId: Int,
) : SqlDriver by driver {
  private val lifecycleMutex = Mutex()
  private var released = false

  /**
   * Closes the SQLite database, waits for the worker acknowledgement, and releases the Worker.
   *
   * Prefer this over [close] when application shutdown must deterministically flush and close the
   * OPFS database.
   */
  suspend fun closeAndAwait() {
    releaseAfter(WorkerActions.close)
  }

  /**
   * Closes and deletes the configured OPFS database, then releases the Worker.
   */
  suspend fun deleteDatabase() {
    releaseAfter(WorkerActions.deleteDatabase)
  }

  /**
   * Immediately releases the Worker without waiting for SQLite's close acknowledgement.
   *
   * Use [closeAndAwait] for deterministic shutdown.
   */
  override fun close() {
    if (released) return
    released = true
    lifecycleWrapper.terminate()
  }

  private suspend fun releaseAfter(action: WorkerAction) {
    lifecycleMutex.withLock {
      if (released) return@withLock

      try {
        lifecycleWrapper.execute(
          WorkerWrapperRequest(
            id = lifecycleRequestId--,
            action = action,
            sql = null,
            statement = null,
          ),
        )
      } finally {
        close()
      }
    }
  }
}

/**
 * Creates a SQLite Wasm OPFS driver after the native worker has opened the configured database.
 *
 * The function returns only after the worker acknowledges a successful configure handshake.
 */
suspend fun createSqliteWasmWebWorkerDriver(
  config: SqliteWasmWorkerConfig = SqliteWasmWorkerConfig(),
): SqliteWasmWebWorkerDriver {
  val worker = createSqliteWasmWorker()
  val lifecycleWrapper = WorkerWrapper(worker)

  try {
    lifecycleWrapper.execute(
      WorkerWrapperRequest(
        id = CONFIGURE_REQUEST_ID,
        action = WorkerActions.configure,
        sql = null,
        statement = null,
        databaseName = config.databaseName,
      ),
    )
  } catch (throwable: Throwable) {
    lifecycleWrapper.terminate()
    throw throwable
  }

  return SqliteWasmWebWorkerDriver(
    driver = WebWorkerDriver(lifecycleWrapper),
    lifecycleWrapper = lifecycleWrapper,
    lifecycleRequestId = FIRST_LIFECYCLE_REQUEST_ID,
  )
}

/**
 * Creates a SQLite Wasm OPFS driver and initializes [schema] before returning.
 *
 * Schema creation or migration and the `PRAGMA user_version` update are performed in the same
 * transaction. Each of [callbacks] runs after its corresponding migration version is complete.
 */
suspend fun createSqliteWasmWebWorkerDriver(
  schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
  config: SqliteWasmWorkerConfig = SqliteWasmWorkerConfig(),
  migrateEmptySchema: Boolean = false,
  vararg callbacks: AfterVersion,
): SqliteWasmWebWorkerDriver {
  val driver = createSqliteWasmWebWorkerDriver(config)
  return try {
    initializeSchema(driver, schema, migrateEmptySchema, callbacks)
    driver
  } catch (throwable: Throwable) {
    try {
      driver.closeAndAwait()
    } catch (_: Throwable) {
      driver.close()
    }
    throw throwable
  }
}

internal expect fun createSqliteWasmWorker(): Worker

private const val CONFIGURE_REQUEST_ID = -1
private const val FIRST_LIFECYCLE_REQUEST_ID = -2
