## Schema Dependencies

You can specify schema dependencies on another module:

=== "Kotlin"
    ```kotlin
    // project-a/build.gradle.kts

    sqldelight {
      databases {
        create("MyDatabase") {
          packageName.set("com.example.projecta")
          dependency(project(":ProjectB"))
        }
      }
    }
    ```
=== "Groovy"
    ```groovy
    // project-a/build.gradle

    sqldelight {
      databases {
        MyDatabase {
          packageName = "com.example.projecta"
          dependency project(":ProjectB")
        }
      }
    }
    ```

This looks for `MyDatabase` in `ProjectB` and includes it's schema when compiling. For this to work,
ProjectB must have a database with the same name (`MyDatabase` in this case) but generate in a
different package, so here is what `ProjectB`'s gradle might look like:

=== "Kotlin"
    ```kotlin hl_lines="4"
    // project-b/build.gradle.kts

    sqldelight {
      databases {
        // Same database name
        create("MyDatabase") {
          package = "com.example.projectb"
        }
      }
    }
    ```
=== "Groovy"
    ```groovy hl_lines="4"
    // project-b/build.gradle

    sqldelight {
      databases {
        // Same database name
        MyDatabase {
          package = "com.example.projectb"
        }
      }
    }
    ```
If you use `deriveSchemaFromMigrations = true`, every module depending on this module must also enable this feature.
