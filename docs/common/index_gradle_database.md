First apply the gradle plugin in your project{% if dialect %} and set your database's dialect accordingly{% endif %}. {% if async %}Make sure to set `generateAsync` to 
`true` when creating your database.{% endif %} 

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
      databases {
        create("Database") {
          packageName.set("com.example"){% if dialect %}
          dialect("{{ dialect }}:{{ versions.sqldelight }}"){% endif %}{% if async %}
          generateAsync.set(true){% endif %}
        }
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
      databases {
        Database { // This will be the name of the generated database class.
          packageName = "com.example"{% if dialect %}
          dialect "{{ dialect }}:{{ versions.sqldelight }}"{% endif %}{% if async %}
          generateAsync = true{% endif %}
        }
      }
    }
    ```
