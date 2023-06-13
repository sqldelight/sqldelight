# Gradle

For greater customization, you can declare databases explicitly using the Gradle DSL.

## SQLDelight Configuration

### `databases`

Container for databases. Configures SQLDelight to create each database with the given name.

=== "Kotlin"
    ```kotlin
    sqldelight {
      databases {
        create("MyDatabase") {
          // Database configuration here.
        }
      }
    }
    ```
=== "Groovy"
    ```groovy
    sqldelight {
      databases {
        MyDatabase {
          // Database configuration here.
        }
      }
    }
    ```

----

### `linkSqlite`

Type: `Property<Boolean>`

For native targets. Whether sqlite should be automatically linked.

Defaults to `true`.

=== "Kotlin"
    ```kotlin
    linkSqlite.set(true)
    ```
=== "Groovy"
    ```groovy
    linkSqlite = true
    ```

## Database Configuration

### `packageName`

Type: `Property<String>`

Package name used for the database class.

=== "Kotlin"
    ```kotlin
    packageName.set("com.example.db")
    ```
=== "Groovy"
    ```groovy
    packageName = "com.example.db"
    ```

----

### `srcDirs`

Type: `ConfigurableFileCollection`

A collection of folders that the plugin will look in for your `.sq` and `.sqm` files.

Defaults to `src/[prefix]main/sqldelight` with prefix depending on the applied kotlin plugin eg common for multiplatform.

=== "Kotlin"
    ```kotlin
    srcDirs.setFrom("src/main/sqldelight")
    ```
=== "Groovy"
    ```groovy
    srcDirs = ['src/main/sqldelight']
    ```

#### `srcDirs(vararg objects: Any)`

A collection of objects that the plugin will look in for your `.sq` and `.sqm` files.

=== "Kotlin"
    ```kotlin
    srcDirs("src/main/sqldelight", "main/sqldelight")
    ```
=== "Groovy"
    ```groovy
    srcDirs('src/main/sqldelight', 'main/sqldelight')
    ```

----

### `schemaOutputDirectory`

Type: `DirectoryProperty`

The directory where `.db` schema files should be stored, relative to the project root.
These files are used to verify that migrations yield a database with the latest schema.

Defaults to `null`.  
If `null`, the migration verification tasks will not be created.

=== "Kotlin"
    ```kotlin
    schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
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

Type: `Property<Boolean>`

If set to true, migration files will fail during the build process if there are any errors in them.

Defaults to `false`.

=== "Kotlin"
    ```kotlin
    verifyMigrations.set(true)
    ```
=== "Groovy"
    ```groovy
    verifyMigrations = true
    ```

----

### `treatNullAsUnknownForEquality`

Type: `Property<Boolean>`

If set to true, SQLDelight will not replace an equality comparison with a nullable typed value when using `IS`.

Defaults to `false`.

=== "Kotlin"
    ```kotlin
    treatNullAsUnknownForEquality.set(true)
    ```
=== "Groovy"
    ```groovy
    treatNullAsUnknownForEquality = true
    ```

----

### `generateAsync`

Type: `Property<Boolean>`

If set to true, SQLDelight will generate suspending query methods for us with asynchronous drivers.

Defaults to `false`.

=== "Kotlin"
    ```kotlin
    generateAsync.set(true)
    ```
=== "Groovy"
    ```groovy
    generateAsync = true
    ```

----

### `deriveSchemaFromMigrations`

Type: `Property<Boolean>`

If set to true, the schema for your database will be derived from your `.sqm` files as if each migration had been applied.
If false, your schema is defined in `.sq` files.

Defaults to `false`.

=== "Kotlin"
    ```kotlin
    deriveSchemaFromMigrations.set(true)
    ```
=== "Groovy"
    ```groovy
    deriveSchemaFromMigrations = true
    ```

{% include 'common/gradle-dependencies.md' %}
