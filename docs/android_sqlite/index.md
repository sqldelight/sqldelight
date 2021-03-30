# Getting Started on Android

First apply the gradle plugin in your project.

```groovy
buildscript {
  repositories {
    google()
    mavenCentral()
    maven { url 'https://www.jetbrains.com/intellij-repository/releases' }
    maven { url "https://jetbrains.bintray.com/intellij-third-party-dependencies" }
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:{{ versions.sqldelight }}'
  }
}

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