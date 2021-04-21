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

## Connection Pools

Disk databases can (optionally) have multiple reader connections, and WAL databases can also (optionally) have 
multiple write/transaction connections. These are advanced configuration features that most users will not need or
benefit from, but they are available in performance-critical situations.

To configure each pool, pass parameters to the various constructors of `NativeSqliteDriver`:

```kotlin
val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db", maxReaderConnections = 4, maxTransactionConnections = 2)
```

Reader connections are only used to run queries outside of a transaction. Any write calls, and anything in a transaction, 
uses a connection from the transaction pool.

In almost all cases, you should avoid `maxTransactionConnections` greater than 1. The database cannot physically
have more than one connection writing at a time, but it definitely can run into potential concurrency issues that can throw
exceptions unexpectedly. Specifically, if you have transactions that overlap, which can happen if you have read statements
early in the transaction, you can get exceptions. It is rare that you'd need this, so basically never change that param
unless you know exactly what it's doing.

However, if you want read transactions, you can have multiple connections in the transaction pool, but you'll want to 
make sure your write transactions are serialized.