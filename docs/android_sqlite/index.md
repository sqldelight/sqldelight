# Getting Started on Android

First apply the gradle plugin in your project's top-level `build.gradle`.

```groovy
buildscript {
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:{{ versions.sqldelight }}'
  }
}
```
Then apply the gradle plugin in your app or module's `build.gradle`.

```groovy
apply plugin: 'com.squareup.sqldelight'
```

{% include 'common/index_schema.md' %}

```groovy
dependencies {
  implementation "com.squareup.sqldelight:android-driver:{{ versions.sqldelight }}"
}
```
```kotlin
val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, context, "test.db")
```

It's recommended to switch Android Studio to use the "Project" view instead of the "Android" view of
your files, in order to find and edit SQLDelight files.

{% include 'common/index_queries.md' %}
