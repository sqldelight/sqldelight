# Getting Started with SQLite on Android

{% include 'common/index_gradle_database.md' %}

!!! tip
    It's recommended to switch Android Studio to use the "Project" view instead of the "Android" 
    view of your files, to make it easier to find and edit SQLDelight files.

{% include 'common/index_schema.md' %}

To use the generated database in your code, you must add the SQLDelight Android driver dependency to 
your project.

=== "Kotlin"
    ```kotlin
    dependencies {
      implementation("app.cash.sqldelight:android-driver:{{ versions.sqldelight }}")
    }
    ```
=== "Groovy"
    ```groovy
    dependencies {
      implementation "app.cash.sqldelight:android-driver:{{ versions.sqldelight }}"
    }
    ```

An instance of the driver can be constructed as shown below, and requires a reference to the generated `Schema` object.
```kotlin
val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, context, "test.db")
```

!!! info
    The `AndroidSqliteDriver` will automatically create or migrate your schema when the driver is constructed.
    Migrations can also be executed manually if needed. See [Code Migrations] for more


{% include 'common/index_queries.md' %}

## SQLite Versions

For Android projects, the SQLDelight Gradle plugin will automatically select the SQLite dialect 
version based on your project's `minSdkVersion` setting. [See here](https://developer.android.com/reference/android/database/sqlite/package-summary) for 
the list of supported SQLite versions on each Android SDK level.

[Code Migrations]: migrations#code-migrations