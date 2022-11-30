# Gradle

For greater customization, you can declare databases explicitly using the Gradle DSL.

## SQLDelight Configuration

### `database`

Configures SQLDelight to create a database with the given name.

=== "Kotlin"
    ```kotlin
    sqldelight {
      database("MyDatabase") {
        // Database configuration here
      }
    }
    ```
=== "Groovy"
    ```groovy
    sqldelight {
      MyDatabase {
        // Database configuration here
      }
    }
    ```

----

### `linkSqlite`

Type: `Boolean`

For native targets. Whether sqlite should be automatically linked.

Defaults to true.

```kotlin
linkSqlite = true
```

## Database Configuration

### `packageName`

Type: `String`

Package name used for the database class.
=== "Kotlin"
    ```kotlin
    packageName = "com.example.db"
    ```
=== "Groovy"
    ```groovy
    packageName = "com.example.db"
    ```

----

### `sourceFolders`

Type: `Collection<String>`

An collection of folders that the plugin will look in for your `.sq` and `.sqm` files.
These folder paths are relative to your existing source set, so if you specify `listOf("db")`
then the plugin will look into `src/main/db` or `src/commondMain/db`.

Defaults to `listOf("sqldelight")`.

=== "Kotlin"
    ```kotlin
    sourceFolders = listOf("db")
    ```
=== "Groovy"
    ```groovy
    sourceFolders = ['db']
    ```
----

### `schemaOutputDirectory`

Type: `File`

The directory where `.db` schema files should be stored, relative to the project root.
These files are used to verify that migrations yield a database with the latest schema.

Defaults to `null`.  
If `null`, the migration verification tasks will not be created.

=== "Kotlin"
    ```kotlin
    schemaOutputDirectory = file("src/main/sqldelight/databases")
    ```
=== "Groovy"
    ```groovy
    schemaOutputDirectory = file("src/main/sqldelight/databases")
    ```

----

### `dependency`

Type: `Project`

Optionally specify schema dependencies on other gradle projects [(see below)](#schema-dependencies).

=== "Kotlin"
    ```kotlin
    dependency(project(":other-project"))
    ```
=== "Groovy"
    ```groovy
    dependency project(":other-project")
    ```

----

### `dialect`

Type: `String` or `Provider<MinimalExternalModuleDependency>`

The SQL dialect you would like to target. Dialects are selected using a gradle dependency.
These dependencies can be specified as `app.cash.sqldelight:{dialect module}:{{ versions.sqldelight }}`. 
See below for available dialects.

For Android projects, the SQLite version is automatically selected based on your `minSdk`. 
Otherwise defaults to SQLite 3.18.

Available dialects:

* HSQL: `hsql-dialect`
* MySQL: `mysql-dialect`
* PostgreSQL: `postgresql-dialect`
* SQLite 3.18: `sqlite-3-18-dialect`
* SQLite 3.24: `sqlite-3-24-dialect`
* SQLite 3.25: `sqlite-3-25-dialect`
* SQLite 3.30: `sqlite-3-30-dialect`
* SQLite 3.33: `sqlite-3-33-dialect`
* SQLite 3.35: `sqlite-3-35-dialect`
* SQLite 3.38: `sqlite-3-38-dialect`

=== "Kotlin"
    ```kotlin
    dialect("app.cash.sqldelight:sqlite-3-24-dialect:{{ versions.sqldelight }}")
    ```
=== "Groovy"
    ```groovy
    dialect 'app.cash.sqldelight:sqlite-3-24-dialect:{{ versions.sqldelight }}'
    ```

----

### `verifyMigrations`

Type: `Boolean`

If set to true, migration files will fail during the build process if there are any errors in them.

Defaults to `false`.

=== "Kotlin"
    ```kotlin
    verifyMigrations = true
    ```
=== "Groovy"
    ```groovy
    verifyMigrations = true
    ```

----

### `treatNullAsUnknownForEquality`

Type: `Boolean`

If set to true, SQLDelight will not replace an equality comparison with a nullable typed value when using `IS`.

Defaults to `false`.

=== "Kotlin"
    ```kotlin
    treatNullAsUnknownForEquality = true
    ```
=== "Groovy"
    ```groovy
    treatNullAsUnknownForEquality = true
    ```

----

### `generateAsync`

Type: `Boolean`

If set to true, SQLDelight will generate suspending query methods for us with asynchronous drivers.

=== "Kotlin"
    ```kotlin
    generateAsync = true
    ```
=== "Groovy"
    ```groovy
    generateAsync = true
    ```

----

### `deriveSchemaFromMigrations`

Type: `Boolean`

If set to true, the schema for your database will be derived from your `.sqm` files as if each migration had been applied.
If false, your schema is defined in `.sq` files.

Defaults to false.

=== "Kotlin"
    ```kotlin
    deriveSchemaFromMigrations = true
    ```
=== "Groovy"
    ```groovy
    deriveSchemaFromMigrations = true
    ```

{% include 'common/gradle-dependencies.md' %}
