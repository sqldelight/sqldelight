# Getting Started with Multiplatform

{% include 'common/index_gradle_database.md' %}

If you are using SQLDelight in single platform mode (e.g. Desktop), add following snippet to `build.gradle`.
And create folder `src/commonMain/sqldelight`.

=== "Kotlin"
    ```kotlin
    kotlin {
      sourceSets {
        val commonMain by getting
      }
    }
    ```
=== "Groovy"
    ```groovy
    kotlin {
      sourceSets {
        commonMain
      }
    }
    ```

Put your SQL statements in a `.sq` file under `src/commonMain/sqldelight`.  
If there's no commonMain folder, just create it.
Typically the first statement in the SQL file creates a table.

```sql
-- src/commonMain/sqldelight/com/example/sqldelight/hockey/data/Player.sq

CREATE TABLE hockeyPlayer (
  player_number INTEGER NOT NULL,
  full_name TEXT NOT NULL
);

CREATE INDEX hockeyPlayer_full_name ON hockeyPlayer(full_name);

INSERT INTO hockeyPlayer (player_number, full_name)
VALUES (15, 'Ryan Getzlaf');
```

From this, SQLDelight will generate a `Database` Kotlin class with an associated `Schema` object that can be used to create your database and run your statements on it. Generating the `Database` file happens during the 'generateSqlDelightInterface' gradle task. This task runs during the 'make project'/'make module' build task, or automatically if you have the SQLDelight IDE plugin.

Accessing the generated database also requires a driver, which SQLDelight provides implementations of:

=== "Kotlin"
    ```kotlin
    kotlin {
      // The drivers needed will change depending on what platforms you target:
    
      sourceSets.androidMain.dependencies {
        implementation("app.cash.sqldelight:android-driver:{{ versions.sqldelight }}")
      }
    
      // or sourceSets.iosMain, sourceSets.windowsMain, etc.
      sourceSets.nativeMain.dependencies {
        implementation("app.cash.sqldelight:native-driver:{{ versions.sqldelight }}")
      }
    
      sourceSets.jvmMain.dependencies {
        implementation("app.cash.sqldelight:sqlite-driver:{{ versions.sqldelight }}")
      }
    }
    ```
=== "Groovy"
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
    }
    ```

```kotlin
// in src/commonMain/kotlin
import com.example.Database

expect class DriverFactory {
  fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): Database {
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

For use with the SqlJs driver, [see here](../js_sqlite/multiplatform).

{% include 'common/index_queries.md' %}
