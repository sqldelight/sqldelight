# Gradle

`build.gradle`:
```groovy
sqldelight {
  // Database name
  MyDatabase {
    // Package name used for the generated MyDatabase.kt
    packageName = "com.example.db"

    // An array of folders where the plugin will read your '.sq' and '.sqm' 
    // files. The folders are relative to the existing source set so if you
    // specify ["db"], the plugin will look into 'src/main/db'. 
    // Defaults to ["sqldelight"] (src/main/sqldelight)
    sourceFolders = ["sqldelight", "resources"]

    // Optionally specify schema dependencies on other gradle projects
    dependency project(':OtherProject')

    // Wether or not to use .sqm files as the source of truth for the schema.
    // Defaults to false
    deriveSchemaFromMigrations = true

    // If set, configures a task to output the .sqm migration files as valid SQL
    // Defaults to null
    migrationOutputDirectory = file("$buildDir/resources/main/migrations")

    // The extension format to use for generated valid SQL migrations.
    // Defaults to ".sql"
    migrationOutputFileFormat = ".sql"
  }
}
```

If you're using Kotlin for your Gradle files:

`build.gradle.kts`
```kotlin
sqldelight {
  database("MyDatabase") {
    packageName = "com.example.db"
    sourceFolders = ["sqldelight", "resources"]
    dependency project(':OtherProject')
    deriveSchemaFromMigrations = true
    migrationOutputDirectory = file("$buildDir/resources/main/migrations")
    migrationOutputFileFormat = ".sql"
  }
}
```

{% include 'common/gradle-dependencies.md' %}
