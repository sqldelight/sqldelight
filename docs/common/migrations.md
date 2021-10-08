# Migrations

The `.sq` file always describes how to create the latest schema in an empty database. If your database is currently on an earlier version, migration files bring those databases up-to-date. 

If the driver supports it, migrations are run in a transaction. You should not surround your migrations in `BEGIN/END TRANSACTION`, as this can cause a crash with some drivers.

## Versioning

The first version of the schema is 1. Migration files are named `<version to upgrade from>.sqm`. To migrate to version 2, put migration statements in `1.sqm`:

```sql
ALTER TABLE hockeyPlayer ADD COLUMN draft_year INTEGER;
ALTER TABLE hockeyPlayer ADD COLUMN draft_order INTEGER;
```

These SQL statements are run by `Database.Schema.migrate()`. Migration files go in the same source set as your `.sq` files.

## Verifying Migrations

You can also place a `.db` file in the `src/main/sqldelight` folder of the same `<version number>.db` format. If there is a `.db` file present, a new `verifySqlDelightMigration` task will be added to the gradle project, and it will run as part of the `test` task, meaning your migrations will be verified against that `.db` file. It confirms that the migrations yield a database with the latest schema.

To generate a `.db` file from your latest schema, run the `generateSqlDelightSchema` task, which is available once you specify a `schemaOutputDirectory`, as described in the [gradle.md](gradle.md). You should probably do this before you create your first migration.

## Code Migrations

If you run your migration from code and would like to perform data migrations you can use the `Database.Schema.migrateWithCallbacks` api:

```kotlin
Database.Schema.migrateWithCallbacks(
    driver = database,
    oldVersion = 0,
    newVersion = Database.Schema.version,
    AfterVersion(3) { it.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) },
)
```

In the following example, if you have 1.sqm, 2.sqm, 3.sqm, 4.sqm, and 5.sqm as migrations, the above callback will happen after 3.sqm completes when the database is on version 4. After the callback it will resume at 4.sqm and complete the remaining migrations, in this case 4.sqm and 5.sqm, meaning the final database version is 6.
