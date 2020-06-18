# Gradle

For greater customization, you can declare databases explicitly using the Gradle DSL.

`build.gradle`:
```groovy
sqldelight {
  // Database name
  MyDatabase {
{% include 'common/gradle-common-groovy-properties.md' %}
  }
  // For native targets, whether sqlite should be automatically linked.
  // Defaults to true.
  linkSqlite = false
}
```

If you're using Kotlin for your Gradle files:

`build.gradle.kts`
```kotlin
sqldelight {
  database("MyDatabase") {
{% include 'common/gradle-common-kotlin-properties.md' %}
  }
  linkSqlite = false
}
```
