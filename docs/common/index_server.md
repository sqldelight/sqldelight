SQLDelight needs to know the schema of your database. If you always create your schema fresh with
create table statements, you'll want to put those statements in `.sq` files,
as shown [here](#fresh-schema). If you apply migration files to an already running database,
you'll want to rename your migration files to `.sqm` files, as shown [here](#migration-schema)

## Fresh Schema

{% include 'common/index_schema_sq.md' %}

In the same `.sq` files you can start placing your sql statements to be executed [at runtime](#typesafe-sql).

## Migration Schema

First, configure gradle to use migrations to assemble the schema:

```groovy
sqldelight {
  Database {
    ...
    sourceFolders = ["sqldelight"]
    deriveSchemaFromMigrations = true
  }
}
```

Migration files have the extension `.sqm`, and must have a number in their file name indicating what
order the migration file runs in. For example, given this hierarchy:

```
src
--main
----sqldelight
------v1__backend.sqm
------v2__backend.sqm
```

SQLDelight will create the schema by applying `v1__backend.sqm` and then `v2__backend.sqm`. Place
your normal SQL `CREATE`/`ALTER` statements in these files. If another service reads from your
migrations files (like flyway), make sure to read the info on [migrations](migrations) and how to
output valid SQL.

## Typesafe SQL

Before you're able to execute SQL statements at runtime, you need to create a `SqlDriver` to connect
to your database. The easiest way is off of a `DataSource` that you would get from hikari or other
connection managers.

```groovy
dependencies {
  implementation "com.squareup.sqldelight:jdbc-driver:{{ versions.sqldelight }}"
}
```
```kotlin
val driver: SqlDriver = dataSource.asJdbcDriver()
```

Regardless of if you specify the schema as fresh create table statements or through migrations,
runtime SQL goes in `.sq` files.

{% include 'common/index_queries.md' %}
