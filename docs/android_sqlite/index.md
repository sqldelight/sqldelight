# SQLDelight

SQLDelight generates typesafe APIs from your SQL statements. It compile-time verifies your schema, statements, and migrations and provides IDE features like autocomplete and refactoring which make writing and maintaining SQL simple. SQLDelight currently supports the SQLite dialect and there are supported SQLite drivers on Android, JVM, iOS, and Windows.

## Example

To use SQLDelight, apply the [gradle plugin](gradle.md) and put your SQL statements in a `.sq` file in `src/main/sqldelight`.  Typically the first statement in the SQL file creates a table.

```sql
-- src/main/sqldelight/com/example/sqldelight/hockey/data/Player.sq

CREATE TABLE hockeyPlayer (
  player_number INTEGER NOT NULL,
  full_name TEXT NOT NULL
);

CREATE INDEX hockeyPlayer_full_name ON hockeyPlayer(full_name);

INSERT INTO hockeyPlayer (player_number, full_name)
VALUES (15, 'Ryan Getzlaf');
```

From this SQLDelight will generate a `Database` Kotlin class with an associated `Schema` object that can be used to create your database and run your statements on it. Doing this also requires a driver, which SQLDelight provides implementations of:

```groovy
dependencies {
  implementation "com.squareup.sqldelight:android-driver:{{ versions.driver }}"
}
```
```kotlin
val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, context, "test.db")
```
