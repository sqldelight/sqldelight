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
Unlike on other platforms, the sqljs driver can not be instantiated directly.
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

{% include 'common/index_queries.md' %}