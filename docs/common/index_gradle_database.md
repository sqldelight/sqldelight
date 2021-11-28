{% include 'common/index_gradle_database_pre_dialect.md' %}
{% include 'common/index_gradle_database_post_dialect.md' %}

If you're using Kotlin for your Gradle files:

```kotlin
plugins {
    id("com.squareup.sqldelight") version "{{ versions.sqldelight }}"
}

apply(plugin = "com.squareup.sqldelight")

repositories {
    google()
    mavenCentral()
}

sqldelight {
    database("Database") {
        packageName = "com.example"
    }
}
```