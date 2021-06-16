# Getting started on Kotlin JS with SQLDelight

{% include 'common/index_gradle_database.md' %}

{% include 'common/index_schema.md' %}

```groovy
kotlin {
  sourceSets.jsMain.dependencies {
    implementation "com.squareup.sqldelight:sqljs-driver:{{ versions.sqldelight }}"
  }
}
```
Unlike on other platforms, the SqlJs driver can not be instantiated directly.
The driver must be loaded asynchronously by calling the `initSqlDriver` function which returns a `Promise<SqlDriver>`.
```kotlin
// As a Promise
val promise: Promise<SqlDriver> = initSqlDriver(Database.Schema)
promise.then { driver -> /* ... */ }

// In a coroutine
suspend fun createDriver() {
    val driver: SqlDriver = initSqlDriver(Database.Schema).await()
    /* ... */
}
```

If building for browsers, some additional webpack configuration is also required.
```js
// project/webpack.conf.d/fs.js
config.resolve = {
    fallback: {
        fs: false,
        path: false,
        crypto: false,
    }
};

// project/webpack.conf.d/wasm.js
const CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: '../../node_modules/sql.js/dist/sql-wasm.wasm',
                to: '../../../{your project}/build/distributions'
            }
        ]
    })
);

```

{% include 'common/index_queries.md' %}