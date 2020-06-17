# Migrations

The `.sq` file always describes how to create the latest schema in an empty database. If your database is currently on an earlier version, migration files bring those databases up-to-date. 

## Versioning

The first version of the schema is 1. Migration files are named `<version to upgrade from>.sqm`. To migrate to version 2, put migration statements in `1.sqm`:

```sql
ALTER TABLE hockeyPlayer ADD COLUMN draft_year INTEGER;
ALTER TABLE hockeyPlayer ADD COLUMN draft_order INTEGER;
```

These SQL statements are run by `Database.Schema.migrate()`.

## Verifying Migrations

You can also place a `.db` file in the `src/main/sqldelight` folder of the same `<version number>.db` format. If there is a `.db` file present, a new `verifySqlDelightMigration` task will be added to the gradle project, and it will run as part of the `test` task, meaning your migrations will be verified against that `.db` file. It confirms that the migrations yield a database with the latest schema.

To generate a `.db` file from your latest schema, run the `generateSqlDelightSchema` task, which is available once you specify a `schemaOutputDirectory`, as described in the [gradle.md](gradle.md). You should probably do this before you create your first migration.
