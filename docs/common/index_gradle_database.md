First apply the gradle plugin in your project.

=== "Kotlin"
    ```kotlin
    plugins {
      id("app.cash.sqldelight") version "{{ versions.sqldelight }}"
    }
     
    repositories {
      google()
      mavenCentral()
    }
    
    sqldelight {
      database("Database") {
        packageName = "com.example"{% if dialect %}
        dialect = "{{ dialect }}:{{ versions.sqldelight }}"{% endif %}
      }
    }
    ```
=== "Groovy"
    ```groovy
    plugins {
      id "app.cash.sqldelight" version "{{ versions.sqldelight }}"
    }

    repositories {
      google()
      mavenCentral()
    }

    sqldelight {
      Database { // This will be the name of the generated database class.
        packageName = "com.example"{% if dialect %}
        dialect = "{{ dialect }}:{{ versions.sqldelight }}"{% endif %}
      }
    }
    ```
