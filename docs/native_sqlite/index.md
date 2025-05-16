# Getting started with SQLDelight on Kotlin/Native

!!! info "Kotlin/Native Memory Manager"
    Since SQLDelight 2.0, the SQLDelight Native driver only supports Kotlin/Native's [new memory manager].

{% include 'common/index_gradle_database.md' %}

{% include 'common/index_schema.md' %}

To use the generated database in your code, you must add the SQLDelight Native driver dependency to
your project.

=== "Kotlin"
    ```kotlin
    kotlin {
      // or iosMain, windowsMain, etc.
      sourceSets.nativeMain.dependencies {
        implementation("app.cash.sqldelight:native-driver:{{ versions.sqldelight }}")
      }
    }
    ```
=== "Groovy"
    ```groovy
    kotlin {
      // or iosMain, windowsMain, etc.
      sourceSets.nativeMain.dependencies {
        implementation "app.cash.sqldelight:native-driver:{{ versions.sqldelight }}"
      }
    }
    ```

An instance of the driver can be constructed as shown below, and requires a reference to the generated `Schema` object.

```kotlin
val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db")
```

{% include 'common/index_queries.md' %}

## Reader Connection Pools

Disk databases can (optionally) have multiple reader connections. To configure the reader pool, pass 
the `maxReaderConnections` parameter to the various constructors of `NativeSqliteDriver`:

```kotlin
val driver: SqlDriver = NativeSqliteDriver(
    Database.Schema, 
    "test.db", 
    maxReaderConnections = 4
)
```

Reader connections are only used to run queries outside of a transaction. Any write calls, and 
anything in a transaction, uses a single connection dedicated to transactions.

[new memory manager]: https://kotlinlang.org/docs/native-memory-manager.html
