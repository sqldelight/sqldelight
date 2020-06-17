# Getting Started on JVM with SQLite

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

sqldelight {
  Database { // This will be the name of the generated database class.
    packageName = "com.example"
  }
}
```

{% include 'common/index_schema.md' %}

```groovy
dependencies {
  implementation "com.squareup.sqldelight:sqlite-driver:{{ versions.sqldelight }}"
}
```
```kotlin
val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
Database.Schema.create(driver)
```

{% include 'common/index_queries.md' %}
