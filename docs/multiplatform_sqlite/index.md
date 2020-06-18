# Getting Started with Multiplatform

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
}
```

```kotlin
// in src/commonMain/kotlin
expect class DriverFactory {
  expect fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory): Database {
  val driver = driverFactory.createDriver()
  val database = Database(driver)

  // Do more work with the database (see below).
}

// in src/androidMain/kotlin
actual class DriverFactory(private val context: Context) {
  actual fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(Database.Schema, context, "test.db") 
  }
}

// in src/nativeMain/kotlin
actual class DriverFactory {
  actual fun createDriver(): SqlDriver {
    return NativeSqliteDriver(Database.Schema, "test.db")
  }
}

// in src/jvmMain/kotlin
actual class DriverFactory {
  actual fun createDriver(): SqlDriver {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    Database.Schema.create(driver)
    return driver
  }
}
```

{% include 'common/index_queries.md' %}