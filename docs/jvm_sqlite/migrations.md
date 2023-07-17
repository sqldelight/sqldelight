{% include 'common/migrations.md' %}

If you are using an `JdbcSqliteDriver` you can pass the schema and callbacks in during the driver's creation.
It uses `PRAGMA user_version` to store current version of schema in database.

```kotlin
val driver: SqlDriver = JdbcSqliteDriver(
    url = "jdbc:sqlite:test.db",
    properties = Properties(),
    schema = Database.Schema,
    callbacks = arrayOf(
        AfterVersion(3) { driver -> driver.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) }
    )
)
```
