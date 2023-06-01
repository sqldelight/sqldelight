# Upgrading to 2.0

SQLDelight 2.0 makes some breaking changes to the gradle plugin and runtime APIs.

This page lists those breaking changes and their new 2.0 equivalents. 
For a full list of new features and other changes, see the [changelog](../changelog).

## New Package Name and Artifact Group

All instances of `com.squareup.sqldelight` need to be replaced with `app.cash.sqldelight`.

```diff title="Gradle Dependencies"
plugins {
-  id("com.squareup.sqldelight") version "{{ versions.sqldelight }}"
+  id("app.cash.sqldelight") version "{{ versions.sqldelight }}"
}

dependencies {
-  implementation("com.squareup.sqldelight:sqlite-driver:{{ versions.sqldelight }}")
+  implementation("app.cash.sqldelight:sqlite-driver:{{ versions.sqldelight }}")
}
```

```diff title="In Code"
-import com.squareup.sqldelight.db.SqlDriver
+import app.cash.sqldelight.db.SqlDriver
```

## Gradle Configuration Changes

* SQLDelight 2.0 requires Java 11 for building, and Java 8 for the runtime.
* The SQLDelight configuration API now uses managed properties and a `DomainObjectCollection` for the databases.

    === "Kotlin"
        ```kotlin 
        sqldelight {
          databases { // (1)!
            create("Database") {
              packageName.set("com.example") // (2)!
            }
          }
        }
        ```
        
        1. New `DomainObjectCollection` wrapper.
        2. Now a `Property<String>`.
    === "Groovy"
        ```kotlin
        sqldelight {
          databases { // (1)!
            Database {
              packageName = "com.example"
            }
          }
        }
        ```
        
        1. New `DomainObjectCollection` wrapper.

* The SQL dialect of your database is now specified using a Gradle dependency.

    === "Kotlin"
        ```groovy
        sqldelight {
          databases {
            create("MyDatabase") {
              packageName.set("com.example")
              dialect("app.cash.sqldelight:mysql-dialect:{{ versions.sqldelight }}")
              
              // Version catalogs also work!
              dialect(libs.sqldelight.dialects.mysql)
            }  
          }  
        }
        ```
    === "Groovy"
        ```groovy
        sqldelight {
          databases {
            MyDatabase {
              packageName = "com.example"
              dialect "app.cash.sqldelight:mysql-dialect:{{ versions.sqldelight }}"
              
              // Version catalogs also work!
              dialect libs.sqldelight.dialects.mysql
            }  
          }  
        }
        ```
    
    The currently supported dialects are `mysql-dialect`, `postgresql-dialect`, `hsql-dialect`, `sqlite-3-18-dialect`, `sqlite-3-24-dialect`, `sqlite-3-25-dialect`, `sqlite-3-30-dialect`, `sqlite-3-35-dialect`, and `sqlite-3-38-dialect`

## Runtime Changes

* Primitive types must now be imported into `.sq` and `.sqm` files.

    ```diff
    +{++import kotlin.Boolean++}
    
    CREATE TABLE HockeyPlayer (
      name TEXT NOT NULL,
      good INTEGER {==As Boolean==}
    );
    ```

    Some previously supported types now require an adapter. Adapters for primitive types are available in the `app.cash.sqldelight:primitive-adapters:{{ versions.sqldelight }}` artifact.
    e.g. The `IntColumnAdapter` for doing `INTEGER As kotlin.Int` conversions.

* The `AfterVersionWithDriver` type was removed in favour of [`AfterVersion`](../2.x/runtime/app.cash.sqldelight.db/-after-version) which now always includes the driver, and the `migrateWithCallbacks` extension function was removed in favour of the main [`migrate`](../2.x/runtime/app.cash.sqldelight.db/-sql-schema/#-775472427%2FFunctions%2F-2112917107) method that now accepts callbacks.

    ```diff
    Database.Schema.{++migrate++}{--WithCallbacks--}(
      driver = driver,
      oldVersion = 1,
      newVersion = Database.Schema.version,
    -  {--AfterVersionWithDriver(3) { driver ->--}
    -  {--  driver.execute(null, "INSERT INTO test (value) VALUES('hello')", 0)--}
    -  {--}--}
    +  {++AfterVersion(3) { driver ->++}
    +  {++  driver.execute(null, "INSERT INTO test (value) VALUES('hello')", 0)++}
    +  {++}++}
    )
    ```

* The `Schema` type is no longer a nested type of `SqlDriver`, and is now called [`SqlSchema`](../2.x/runtime/app.cash.sqldelight.db/-sql-schema).

    ```diff
    -val schema: {--SqlDriver.Schema--}
    +val schema: {++SqlSchema++}
    ```
  
* The [paging3 extension API](../2.x/extensions/androidx-paging3/app.cash.sqldelight.paging3/) has changed to only allow int types for the count.
* The [coroutines extension API](../2.x/extensions/coroutines-extensions/app.cash.sqldelight.coroutines/) now requires a dispatcher to be explicitly passed in.
    ```diff
    val players: Flow<List<HockeyPlayer>> =
      playerQueries.selectAll()
        .asFlow()
    +   .mapToList({++Dispatchers.IO++})
    ```
* Some driver methods like [`execute()`](../2.x/runtime/app.cash.sqldelight.db/-sql-driver/execute), [`executeQuery()`](../2.x/runtime/app.cash.sqldelight.db/-sql-driver/execute-query), `newTransaction()`, and `endTransaction()` now return a [`QueryResult`](../2.x/runtime/app.cash.sqldelight.db/-query-result) object. Use [`QueryResult.value`](../2.x/runtime/app.cash.sqldelight.db/-query-result/value) to access the returned value.
    ```diff
    -driver.executeQuery(null, "PRAGMA user_version", { /*...*/ })
    +driver.executeQuery(null, "PRAGMA user_version", { /*...*/ }){++.value++}
    ```
    This change enables driver implementations to be based on non-blocking APIs where the returned value can be accessed using the suspending [`QueryResult.await()`](../2.x/runtime/app.cash.sqldelight.db/-query-result/await) method.
