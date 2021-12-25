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

{% include 'common/index_queries.md' %}

## Reader Connection Pools

Disk databases can (optionally) have multiple reader connections. To configure the reader pool, pass the `maxReaderConnections` parameter to the various constructors of `NativeSqliteDriver`:

```kotlin
val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db", maxReaderConnections = 4)
```

Reader connections are only used to run queries outside of a transaction. Any write calls, and anything in a transaction, 
uses a singe connection dedicated to transactions.
