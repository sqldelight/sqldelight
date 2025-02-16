# Migrations

An `.sq` file always describes how to create the latest schema in an empty database. If your database is currently on an earlier version, migration files bring those databases up-to-date. Migration files are stored in the same `sqldelight` folder as your `.sq` files:

```
src
└─ main
   └─ sqdelight
      ├─ com/example/hockey
      |  ├─ Team.sq
      |  └─ Player.sq
      └─ migrations
         ├─ 1.sqm
         └─ 2.sqm
```

If the driver supports it, migrations are run in a transaction. You should not surround your migrations in `BEGIN/END TRANSACTION`, as this can cause a crash with some drivers.

## Versioning

The first version of the schema is 1. Migration files are named `<version to upgrade from>.sqm`. To migrate to version 2, put migration statements in `1.sqm`:

```sql
ALTER TABLE hockeyPlayer ADD COLUMN draft_year INTEGER;
ALTER TABLE hockeyPlayer ADD COLUMN draft_order INTEGER;
```

These SQL statements are run by the `Database.Schema.migrate()` method. Migration files go in the same source set as your `.sq` files.

## Verifying Migrations

A `verifySqlDelightMigration` task will be added to the gradle project, and it will run as part of the `check` task. For any `.db` file named `<version number>.db` in your SqlDelight source set (e.g. `src/main/sqldelight`) it will apply all migrations starting from `<version number>.sqm`, and confirms that the migrations yield a database with the latest schema.

To generate a `.db` file from your latest schema, run the `generate<database name>Schema` task, which is available once you specify a `schemaOutputDirectory`, as described in the [gradle.md](gradle.md). You should probably do this before you create your first migration.

Most use cases would benefit from only having a `1.db` file representing the schema of the initial version of their database. Having multiple `.db` files is allowed, but that would result in each `.db` file having each of its migrations applied to it, which causes a lot of unnecessary work.

## Code Migrations

If you run your migration from code and would like to perform data migrations you can use the `Database.Schema.migrate` api:

```kotlin
Database.Schema.migrate(
    driver = database,
    oldVersion = 0,
    newVersion = Database.Schema.version,
    AfterVersion(3) { driver -> driver.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) },
)
```

In the following example, if you have 1.sqm, 2.sqm, 3.sqm, 4.sqm, and 5.sqm as migrations, the above callback will happen after 3.sqm completes when the database is on version 4. After the callback it will resume at 4.sqm and complete the remaining migrations, in this case 4.sqm and 5.sqm, meaning the final database version is 6.
