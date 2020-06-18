# Getting Started on JVM with SQLite

{% include 'common/index_gradle_database.md' %}

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
