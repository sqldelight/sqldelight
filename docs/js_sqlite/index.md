---
async: true
---
# Getting started with SQLDelight on Kotlin/JS

!!! info
    The synchronous `sqljs-driver` (pre-2.0) has been replaced with the asynchronous `web-worker-driver`.
    This requires configuring the `generateAsync` setting in your Gradle configuration.

{% include 'common/index_gradle_database.md' %}

{% include 'common/index_schema.md' %}

=== "Kotlin"
    ```kotlin
    kotlin {
      sourceSets.jsMain.dependencies {
        implementation("app.cash.sqldelight:web-worker-driver:{{ versions.sqldelight }}")
        implementation(devNpm("copy-webpack-plugin", "9.1.0"))
      }
    }
    ```
=== "Groovy"
    ```groovy
    kotlin {
      sourceSets.jsMain.dependencies {
        implementation "app.cash.sqldelight:web-worker-driver:{{ versions.sqldelight }}"
        implementation devNpm("copy-webpack-plugin", "9.1.0")
      }
    }
    ```

The web worker driver allows SQLDelight to communicate with a SQL implementation that is running in
a [Web Worker]. This allows all database operations to happen in a background process. SQLDelight
provides both an in-memory [SQL.js Worker] and a persistent [SQLite Wasm OPFS Worker] backed by the
official SQLite Wasm build.

!!! info
    The web worker driver is only compatible with browser targets. 

## Configuring a Web Worker

SQLDelight's web worker driver isn't tied to a specific implementation of a worker. Instead the
driver communicates with the worker using a standardized set of messages. SQLDelight provides an
in-memory worker that uses [SQL.js] and a persistent worker that uses SQLite's standard OPFS VFS.

See the [SQL.js Worker] or [SQLite Wasm OPFS Worker] page for setup details, or the [Custom Workers]
page for details on implementing your own.

## Using a Web Worker

When creating a driver for SQL.js or a custom worker, pass a reference to the worker that will handle
all SQL operations. The `Worker` constructor accepts a `URL` object that references the worker
script. The [SQLite Wasm OPFS Worker] helper creates its dedicated module worker automatically.

Webpack has special support for referencing a worker script from an installed NPM package by passing
`import.meta.url` as a second argument to the `URL` constructor. Webpack will automatically bundle
the worker script from the referenced NPM package at build time. The example below shows a Worker
being created from SQLDelight's [SQL.js Worker].

```kotlin
val driver = WebWorkerDriver(
  Worker(
    js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")
  )
)
```

!!! warning
    In order for Webpack to correctly resolve this URL while bundling, you must construct the `URL` 
    object entirely within a `js()` block as shown above with the `import.meta.url` argument.

From here, you can use the driver like any other SQLDelight driver.

## Using Queries

{% include 'common/index_queries.md' %}

[Web Worker]: https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API/Using_web_workers
[SQL.js]: https://github.com/sql-js/sql.js/
[SQL.js Worker]: sqljs_worker.md
[SQLite Wasm OPFS Worker]: sqlite_wasm_worker.md
[Custom Workers]: custom_worker.md
