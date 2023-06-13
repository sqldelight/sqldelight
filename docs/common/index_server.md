SQLDelight needs to know the schema of your database. There are typically two approaches to setting up your database's 
schema. The "Fresh Schema" approach assumes that you are starting with an empty database, and that all the statements 
necessary to bring it to the desired state will be applied all at once. The "Migration Schema" approach on the other 
hand assumes that you already have a database and schema set up (e.g. an existing production database), and that you'll 
progressively apply migrations over time to update your database's schema.

In SQLDelight, these approaches translate to either writing your table definitions in `.sq` files for a 
"[Fresh Schema](#fresh-schema)", or by writing migration statements in `.sqm` files for a "[Migration Schema](#migration-schema)".
In both cases, your SQL _queries_ will be written in `.sq` files ([as shown here](#typesafe-sql)).

## Fresh Schema

{% include 'common/index_schema_sq.md' %}

In the same `.sq` files you can start placing your sql statements to be executed [at runtime](#typesafe-sql).

## Migration Schema

First, configure gradle to use migrations to assemble the schema:

=== "Kotlin"
    ```kotlin
    sqldelight {
      databases {
        create("Database") {
          ...
          srcDirs("sqldelight")
          deriveSchemaFromMigrations.set(true)
        }
      }
    }
    ```
=== "Groovy"
    ```groovy
    sqldelight {
      databases {
        Database {
          ...
          srcDirs "sqldelight"
          deriveSchemaFromMigrations = true
        }
      }
    }
    ```

Migration files have the extension `.sqm`, and must have a number in their file name indicating what
order the migration file runs in. For example, given this hierarchy:

```
src
`-- main
    `-- sqldelight
        |-- v1__backend.sqm
        `-- v2__backend.sqm
```

SQLDelight will create the schema by applying `v1__backend.sqm` and then `v2__backend.sqm`. Place
your normal SQL `CREATE`/`ALTER` statements in these files. If another service reads from your
migrations files (like flyway), make sure to read the info on [migrations](migrations) and how to
output valid SQL.

## Typesafe SQL

Before you're able to execute SQL statements at runtime, you need to create a `SqlDriver` to connect
to your database. The easiest way is off of a `DataSource` that you would get from hikari or other
connection managers.

=== "Kotlin"
    ```kotlin
    dependencies {
      implementation("app.cash.sqldelight:jdbc-driver:{{ versions.sqldelight }}")
    }
    ```
=== "Groovy"
    ```groovy
    dependencies {
      implementation "app.cash.sqldelight:jdbc-driver:{{ versions.sqldelight }}"
    }
    ```
```kotlin
val driver: SqlDriver = dataSource.asJdbcDriver()
```

Regardless of if you specify the schema as fresh create table statements or through migrations,
runtime SQL goes in `.sq` files.

{% include 'common/index_queries.md' %}
