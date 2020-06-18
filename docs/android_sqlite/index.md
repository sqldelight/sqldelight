# Getting Started on Android

First apply the gradle plugin in your project.

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

{% include 'common/index_queries.md' %}