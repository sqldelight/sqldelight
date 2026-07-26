# SQLite Wasm OPFS Web Worker

SQLDelight's SQLite Wasm worker runs the official [SQLite Wasm] build in a dedicated ECMAScript
module Web Worker. It opens the configured database with SQLite's standard `OpfsDb` VFS, which
stores SQLite pages in the browser's origin-private file system (OPFS).

The [SQL.js Worker] keeps its database in memory and loses it when the page goes away. This worker
instead lets SQLite write its own pages to a real file, so a committed transaction is durable and
survives a reload without exporting and rewriting the whole database.

!!! warning
    This worker intentionally fails during initialization when standard OPFS support is unavailable.
    It does not fall back to IndexedDB or silently create an in-memory database.

## Dependencies

Add the web worker driver, the SQLDelight SQLite Wasm worker package at the same SQLDelight version,
and its official SQLite Wasm peer dependency:

=== "Kotlin/JS"
    ```kotlin
    kotlin {
      sourceSets.jsMain.dependencies {
        implementation("app.cash.sqldelight:web-worker-driver:{{ versions.sqldelight }}")
        implementation(npm("@cashapp/sqldelight-sqlite-wasm-worker", "{{ versions.sqldelight }}"))
        implementation(npm("@sqlite.org/sqlite-wasm", "3.53.0-build1"))
      }
    }
    ```
=== "Kotlin/Wasm and KMP"
    ```kotlin
    kotlin {
      sourceSets.wasmJsMain.dependencies {
        implementation("app.cash.sqldelight:web-worker-driver:{{ versions.sqldelight }}")
        implementation(npm("@cashapp/sqldelight-sqlite-wasm-worker", "{{ versions.sqldelight }}"))
        implementation(npm("@sqlite.org/sqlite-wasm", "3.53.0-build1"))
      }
    }
    ```

For a multiplatform project with both browser targets, add the same dependencies to both `jsMain`
and `wasmJsMain`.

The web worker driver is asynchronous, so enable asynchronous SQLDelight generation:

```kotlin
sqldelight {
  databases {
    create("Database") {
      packageName.set("com.example.db")
      generateAsync.set(true)
    }
  }
}
```

## Source set layout

Driver dependencies are declared in `jsMain` and `wasmJsMain`, but the SQLDelight Gradle plugin does
not read `.sq` and `.sqm` files from those source sets.

A project applying `org.jetbrains.kotlin.multiplatform` keeps them in `src/commonMain/sqldelight`,
even when `js` or `wasmJs` is its only target. A project applying `org.jetbrains.kotlin.js` keeps
them in `src/main/sqldelight`.

```sql title="src/commonMain/sqldelight/com/example/db/Player.sq"
CREATE TABLE hockeyPlayer (
  player_number INTEGER PRIMARY KEY NOT NULL,
  full_name TEXT NOT NULL
);
```

The generated database code is added to that same source set, so every target can see it.

## Browser and server requirements

SQLite's standard `OpfsDb` VFS requires all of the following:

* A browser that supports OPFS and `FileSystemSyncAccessHandle`.
* A secure context. Use HTTPS in production; browsers also treat `localhost` as secure for
  development.
* A dedicated Web Worker. SQLDelight creates the required module worker automatically.
* `SharedArrayBuffer`, which requires the page to be [cross-origin isolated].
* These HTTP response headers on the top-level document and application responses:

    ```text
    Cross-Origin-Opener-Policy: same-origin
    Cross-Origin-Embedder-Policy: require-corp
    ```

For local development, Kotlin's webpack dev server loads additional configuration from
`webpack.config.d`. For example:

```js title="webpack.config.d/opfs.js"
config.devServer = {
  ...(config.devServer || {}),
  headers: {
    ...((config.devServer && config.devServer.headers) || {}),
    "Cross-Origin-Opener-Policy": "same-origin",
    "Cross-Origin-Embedder-Policy": "require-corp",
  },
};
```

This configuration only affects the development server. In production, your HTTP server, hosting
platform, or CDN must send the two headers.

Cross-origin isolation changes how the whole page can interact with other origins. With
`Cross-Origin-Embedder-Policy: require-corp`, third-party scripts, images, fonts, frames, and other
resources must opt in through CORS or an appropriate `Cross-Origin-Resource-Policy` header.
`Cross-Origin-Opener-Policy: same-origin` separates cross-origin popups into another browsing
context group, so integrations that depend on `window.opener` can stop working. Review and test
analytics, authentication, payment, and other third-party integrations before enabling these
headers in production.

## Creating the driver

`createSqliteWasmWebWorkerDriver` creates the module worker and waits until it has opened the named
OPFS database:

```kotlin
import app.cash.sqldelight.driver.worker.createSqliteWasmWebWorkerDriver

val driver = createSqliteWasmWebWorkerDriver(databaseName = "app.db")
```

`databaseName` is relative to the current origin's OPFS root. The default is `sqldelight.db`.
Storage is isolated by browser origin, so the same name on another origin identifies a different
database.

Most applications should use the schema overload so a new database is created or an existing one
is migrated before the driver is returned:

```kotlin
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.driver.worker.createSqliteWasmWebWorkerDriver
import com.example.db.Database

val callbacks = arrayOf(
  AfterVersion(1) {
    println("Migrated the OPFS database to version 1")
  },
)

val driver = createSqliteWasmWebWorkerDriver(
  schema = Database.Schema,
  databaseName = "app.db",
  migrateEmptySchema = false,
  callbacks = callbacks,
)
```

With the default `migrateEmptySchema = false`, an empty database is initialized with
`Database.Schema.create`. Set it to `true` to migrate an empty database from version 0 instead.
`AfterVersion` callbacks run after their corresponding migration version. Schema creation or
migration and the `PRAGMA user_version` update occur in one transaction.

## Closing and deleting

Use the suspending lifecycle methods when the result must be known:

```kotlin
driver.closeAndAwait()
```

`closeAndAwait()` waits for SQLite to close the OPFS database and acknowledge the request before
the worker is released. The inherited synchronous `close()` terminates the worker immediately and
cannot wait for that acknowledgement.

To remove the database, call `deleteDatabase()` on its open driver:

```kotlin
driver.deleteDatabase()
```

`deleteDatabase()` closes the database, removes its file from OPFS, and releases the worker.
Lifecycle calls are idempotent on a driver instance. If `close()` or `closeAndAwait()` has already
released that instance, a later `deleteDatabase()` call on it does nothing; open a driver for the
same database name and delete that instance instead.

## Concurrency

Kotlin/JS and Kotlin/Wasm run your application on a single thread, so coroutines in the page
interleave but never execute in parallel. The Worker is what actually runs SQLite off that thread,
and it handles one request at a time in the order it receives them. A query therefore never blocks
the page, but two queries issued from two coroutines still run one after the other.

Because the driver tracks the current transaction for the connection it owns, run a transaction to
completion before starting another one on the same driver.

## Multiple tabs

Each driver owns one Worker holding one SQLite connection, so a second tab of your application is
simply a second connection to the same OPFS file. Committed writes are visible to the other
connection on its next read.

Only one connection can write at a time. The OPFS file is locked for the duration of a write
transaction, and the losing connection blocks inside its own Worker and retries until the lock is
released. The worker sets `PRAGMA busy_timeout = 5000` so ordinary contention resolves by waiting
rather than failing; a write that cannot get the lock within that window fails with `SQLITE_BUSY` or
an I/O error. Keep write transactions short, and raise or lower the timeout for your workload by
executing `PRAGMA busy_timeout` through the driver.

SQLite's own [persistence documentation] notes that roughly 8-10 concurrent connections are
practical when locking is kept brief. Deleting a database requires that no other connection has it
open.

[SQLite Wasm]: https://sqlite.org/wasm/doc/trunk/index.md
[SQL.js Worker]: sqljs_worker.md
[persistence documentation]: https://sqlite.org/wasm/doc/trunk/persistence.md
[cross-origin isolated]: https://developer.mozilla.org/en-US/docs/Web/API/Window/crossOriginIsolated
