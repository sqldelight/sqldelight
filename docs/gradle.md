# Gradle

```groovy
buildscript {
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:1.2.1'
  }
}

apply plugin: 'com.squareup.sqldelight'
```

For Android projects, the plugin will create a default database called `Database` using the project package name. For greater customization, you can declare databases explicitly using the Gradle DSL.

`build.gradle`:
```groovy
sqldelight {
  MyDatabase {
    //package name used for the generated MyDatabase.kt
    packageName = "com.example.db"

    // An array of folders where the plugin will read your '.sq' and '.sqm' files.
    // The folders are relative to the existing source set so if you specify ["db"],
    // the plugin will look into 'src/main/db'
    // Defaults to ["sqldelight"] (src/main/sqldelight)
    sourceFolders = ["db"]

    // The directory where to store '.db' schema files relative to the root of the project.
    // These files are used to verify that migrations yield a database with the latest schema.
    // Defaults to null so the verification tasks will not be created.
    schemaOutputDirectory = file("src/main/sqldelight/databases")

    // Optionally specify schema dependencies on other gradle projects
    dependency project(':OtherProject')
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
    packageName = "com.example.db"
    sourceFolders = listOf("db")
    schemaOutputDirectory = file("build/dbs")
    dependency(project(":OtherProject"))
  }
  linkSqlite = false
}
```

## Snapshots

Snapshots of the development version (including the IDE plugin zip) are available in
[Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/).

## Upgrading From Previous Versions

There's a separate guide for upgrading from 0.7 and other pre-1.0 versions [here](/upgrading)
