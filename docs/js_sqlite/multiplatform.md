---
async: true
---
# Multiplatform setup with the Web Worker Driver

{% include 'common/index_gradle_database.md' %}

{% include 'common/index_schema.md' %}

```groovy
kotlin {
  // The drivers needed will change depending on what platforms you target:

  sourceSets.androidMain.dependencies {
    implementation "app.cash.sqldelight:android-driver:{{ versions.sqldelight }}"
  }

  // or sourceSets.iosMain, sourceSets.windowsMain, etc.
  sourceSets.nativeMain.dependencies {
    implementation "app.cash.sqldelight:native-driver:{{ versions.sqldelight }}"
  }

  sourceSets.jvmMain.dependencies {
    implementation "app.cash.sqldelight:sqlite-driver:{{ versions.sqldelight }}"
  }

  sourceSets.jsMain.dependencies {
    implementation "app.cash.sqldelight:sqljs-driver:{{ versions.sqldelight }}"
    implementation npm("sql.js", "1.6.2")
    implementation devNpm("copy-webpack-plugin", "9.1.0")
  }
}
```

## Creating Drivers

First set up a way to create a driver in your common code. This can be done using `expect`/`actual`,
or simply with a common interface a platform-specific implementations of the interface.

```kotlin title="src/commonMain/kotlin"
expect suspend fun provideDbDriver(
  schema: SqlSchema<QueryResult.AsyncValue<Unit>>
): SqlDriver
```
The `SqlSchema` interface contains a generic `QueryResult` type argument which is used to differentiate
schema code that is generated with the `generateAsync` configuration option set to `true`.
Some drivers rely on synchronous behaviours when creating or migrating the schema, so to use an
asynchronous schema you can use the [`synchronous()`](../../2.x/extensions/async-extensions/app.cash.sqldelight.async.coroutines/#427896482%2FFunctions%2F-1043631958) extension method to adapt it for use with synchronous drivers. 

=== "src/jsMain/kotlin"
    ```kotlin
    actual suspend fun provideDbDriver(
      schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver {
      return WebWorkerDriver(
        Worker(
          js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")
        )
      ).also { schema.create(it).await() }
    }
    ```
=== "src/jvmMain/kotlin"
    ```kotlin
    actual suspend fun provideDbDriver(
      schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver {
      return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        .also { schema.create(it).await() }
    }
    ```
=== "src/androidMain/kotlin"
    ```kotlin
    actual suspend fun provideDbDriver(
      schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver {
      return AndroidSqliteDriver(schema.synchronous(), context, "test.db")
    }
    ```
=== "src/nativeMain/kotlin"
    ```kotlin
    actual suspend fun provideDbDriver(
      schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver {
      return NativeSqliteDriver(schema.synchronous(), "test.db")
    }
    ```
