# Getting started on Kotlin Native with SQLDelight

{% include 'common/index_gradle_database.md' %}

{% include 'common/index_schema.md' %}

```groovy
kotlin {
  // or sourceSets.iosMain, sourceSets.windowsMain, etc.
  sourceSets.nativeMain.dependencies {
    implementation "com.squareup.sqldelight:native-driver:{{ versions.sqldelight }}"
  }
}
```
```kotlin
val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db")
```

{% include 'common/index_queries.md' %}

## Connection Pool

Databases running in WAL mode (the default) can have multiple connections in a pool. To enable
the conneciton pool, initializes the driver with the `maxConcurrentConnections`:

```kotlin
val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db", maxConcurrentConnections = 4)
```

This will enable multiple concurrent connections, which will support reading while there is a write
transaction. It will also support multiple reading threads, and read transactions. However, this is an advanced feature, and 
should be enabled only if necessary. Multiple transactions that overlap and both read and write 
can cause data integrity issues and will throw an exception.

*It is highly recommended to do all writes in one connection*. You can safely read from other connections 
while the other is writing. Because it is possible to throw an exception if multiple connections have overlapping
write transactions, you should write from one thread, or catch and retry your transaction.

The default is a single connection, and this is recommended in pretty much all cases, unless concurrent
read performance is critical.