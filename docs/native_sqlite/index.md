# Getting started on Kotlin Native with SQLDelight

{% include 'common/index_gradle_database.md' %}

{% include 'common/index_schema.md' %}

```groovy
//Groovy
kotlin {
  // or sourceSets.iosMain, sourceSets.windowsMain, etc.
  sourceSets.nativeMain.dependencies {
    implementation "com.squareup.sqldelight:native-driver:{{ versions.sqldelight }}"
  }
}
```
```kotlin
//Kotlin
kotlin {
  // or sourceSets.iosMain, sourceSets.windowsMain, etc.
  sourceSets.nativeMain.dependencies {
    implementation ("com.squareup.sqldelight:native-driver:{{ versions.sqldelight }}")
  }
}
```
```kotlin
val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db")
```

### Native Driver Modules

There are two native driver modules. One, `native-driver`, is for the newer memory model. `native-driver-strict`
provides support for the original strict memory model.

Projects that continue to use the strict memory model should switch to `native-driver-strict`. The Kotlin/Native platform
and libraries from Jetbrains are generally focused on the new memory model going forward. Moving to the new memory will be
generally recommended in the near future, as libraries like kotlinx.coroutines are likely to drop support for the strict
memory model around the release of Kotlin 1.7.

For strict memory support, replace the dependency above with:

```kotlin
implementation ("com.squareup.sqldelight:native-driver-strict:{{ versions.sqldelight }}")
```

{% include 'common/index_queries.md' %}

## Reader Connection Pools

Disk databases can (optionally) have multiple reader connections. To configure the reader pool, pass the `maxReaderConnections` parameter to the various constructors of `NativeSqliteDriver`:

```kotlin
val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db", maxReaderConnections = 4)
```

Reader connections are only used to run queries outside of a transaction. Any write calls, and anything in a transaction, 
uses a single connection dedicated to transactions.
