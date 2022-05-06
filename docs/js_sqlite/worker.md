# Running in a Web Worker

To use the SqlJs driver with sql.js running in a web worker, first update your gradle configuration
to generate asynchronous SQLDelight interfaces.

```groovy
sqldelight {
    Database {
        packageName = "com.example"
        generateAsync = true
    }
}
```

The web worker driver only works with browser targets.
In addition to the base webpack configuration needed for the SqlJs driver, the web worker script must also be copied.

```js
// project/webpack.config.d/wasm.js
const CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            '../../node_modules/sql.js/dist/sql-wasm.wasm',
            '../../node_modules/sql.js/dist/worker.sql-wasm.js'
        ]
    })
);
```

The function to initialize the driver is a `suspend` function and must be called within a coroutine.
Since this driver is asynchronous, all query functions are also `suspend` functions.

```kotlin
suspend fun setupDb(): AsyncSqlDriver {
    return initAsyncSqlDriver(schema = Database.Schema)
}

suspend fun getData(database: Database): List<MyData> {
    return database.myDataQueries.getAll().executeAsList()
}
```

## Custom Workers

By default, the js worker driver will load a web worker from `/worker.sql-wasm.js` (the additional file copied in the webpack config).
The driver supports loading custom workers, or workers located at a different path by providing the URL to the `initAsyncSqlDriver()` function.

```kotlin
initAsyncSqlDriver(workerPath = "/path/to/my/worker.js")
```

Custom workers must be able to receive and process messages sent by the driver that are defined in the [`WorkerMessage`](1.x/sqljs-driver/app.cash.sqldelight.app.cash.sqldelight.driver.sqljs.worker/-worker-message/) interface.
The driver expects a responding message to be sent that matches the [`WorkerData`](1.x/sqljs-driver/app.cash.sqldelight.app.cash.sqldelight.driver.sqljs.worker/-worker-data/) interface.

## Testing

To run karma tests with the web worker, the karma config must be updated to copy and proxy the worker script file in addition to the WebAssembly file.

```js
// project/karma.config.d/wasm.js
const path = require("path");
const dist = path.resolve("../../node_modules/sql.js/dist/")
const wasm = path.join(dist, "sql-wasm.wasm")
const worker = path.join(dist, "worker.sql-wasm.js")

config.files.push({
    pattern: wasm,
    served: true,
    watched: false,
    included: false,
    nocache: false,
}, {
    pattern: worker,
    served: true,
    watched: false,
    included: false,
    nocache: false,
});

config.proxies["/sql-wasm.wasm"] = `/absolute${wasm}`
config.proxies["/worker.sql-wasm.js"] = `/absolute${worker}`
```
