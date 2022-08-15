# Getting Started on Android

{% include 'common/index_gradle_database.md' %}

{% include 'common/index_schema.md' %}

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
```kotlin
val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, context, "test.db")
```

It's recommended to switch Android Studio to use the "Project" view instead of the "Android" view of
your files, in order to find and edit SQLDelight files.

{% include 'common/index_queries.md' %}
