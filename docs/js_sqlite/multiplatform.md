# Multiplatform setup with the SqlJs Driver

{% include 'common/index_gradle_database.md' %}

{% include 'common/index_schema.md' %}

```groovy
kotlin {
  // The drivers needed will change depending on what platforms you target:

  sourceSets.androidMain.dependencies {
    implementation "com.squareup.sqldelight:android-driver:{{ versions.sqldelight }}"
  }

  // or sourceSets.iosMain, sourceSets.windowsMain, etc.
  sourceSets.nativeMain.dependencies {
    implementation "com.squareup.sqldelight:native-driver:{{ versions.sqldelight }}"
  }

  sourceSets.jvmMain.dependencies {
    implementation "com.squareup.sqldelight:sqlite-driver:{{ versions.sqldelight }}"
  }

  sourceSets.jsMain.dependencies {
    implementation "com.squareup.sqldelight:sqljs-driver:{{ versions.sqldelight }}"
    implementation npm("sql.js", "1.6.2")
    implementation devNpm("copy-webpack-plugin", "9.1.0")
  }
}
```

Because the SqlJs driver must be initialized asynchronously, the drivers for other platforms must be initialized in a compatible way to be usable in a common source set.

The drivers can be initialized in a coroutine, and a higher-order function can be used to ensure that the driver is initialized before executing a block of code that requires the database:

```kotlin
// in src/commonMain/kotlin
expect suspend fun provideDbDriver(schema: SqlDriver.Schema): SqlDriver

class SharedDatabase(
    private val driverProvider: suspend (SqlDriver.Schema) -> SqlDriver
) {
    private var database: Database? = null

    suspend fun initDatabase() {
        if (database == null) {
            database = driverProvider(Database.Schema).createDatabase()
        }
    }

    suspend operator fun <R> invoke(block: suspend (Database) -> R): R {
        initDatabase()
        return block(database!!)
    }

    private fun SqlDriver.createDatabase(): Database { /* ... */ }
}

val sharedDb = SharedDatabase(::createTestDbDriver)
class DataRepository(
    private val withDatabase: SharedDatabase = sharedDb
) {
    suspend fun getData() = withDatabase { database ->
        /* Do something with the database */
    }
}

// in src/jsMain/kotlin
actual suspend fun provideDbDriver(schema: SqlDriver.Schema): SqlDriver {
    return initSqlDriver(schema).await()
}

// in src/nativeMain/kotlin
actual suspend fun provideDbDriver(schema: SqlDriver.Schema): SqlDriver {
    return NativeSqliteDriver(schema, "test.db")
}

// in src/jvmMain/kotlin
actual suspend fun provideDbDriver(schema: SqlDriver.Schema): SqlDriver {
    return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { driver ->
        schema.create(driver)
    }
}
```
