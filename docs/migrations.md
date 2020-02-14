# Migrations

The `.sq` file always describes how to create the latest schema in an empty database. If your database is currently on an earlier version, migration files bring those databases up-to-date. 

The first version of the schema is 1. Migration files are named `<version to upgrade from>.sqm`. To migrate to version 2, put migration statements in `1.sqm`:

```sql
ALTER TABLE hockeyPlayer ADD COLUMN draft_year INTEGER;
ALTER TABLE hockeyPlayer ADD COLUMN draft_order INTEGER;
```

Migration files go in the `src/main/sqldelight` folder. 

These SQL statements are run by `Database.Schema.migrate()`. This is automatic for the Android and iOS drivers. 

You can also place a `.db` file in the `src/main/sqldelight` folder of the same `<version number>.db` format. If there is a `.db` file present, a new `verifySqlDelightMigration` task will be added to the gradle project, and it will run as part of the `test` task, meaning your migrations will be verified against that `.db` file. It confirms that the migrations yield a database with the latest schema.

To generate a `.db` file from your latest schema, ensure you have set `schemaOutputDirectory` in the [Gradle config](gradle.md), then run the `generateSqlDelightSchema` task. You should probably do this before you create your first migration.
