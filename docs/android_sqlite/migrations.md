{% include 'common/migrations.md' %}

If you are using an `AndroidSqliteDriver` you can pass these callbacks in during the driver's creation:

```kotlin
val driver: SqlDriver = AndroidSqliteDriver(
    schema = Database.Schema,
    context = context,
    name = "test.db",
    callback = AndroidSqliteDriver.Callback(
        schema = Database.Schema,
        AfterVersion(3) { database.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) },
    )
)
```
