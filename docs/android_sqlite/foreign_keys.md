{% if multiplatform %}
## Android SQLite
{% else %}
# Foreign Keys
{% endif %}

You can enable foreign key constraints for the Android SQLite driver through the driver's `onOpen` callback.

```kotlin
AndroidSqliteDriver(
  schema = Database.Schema,
  callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
    override fun onOpen(db: SupportSQLiteDatabase) {
      db.setForeignKeyConstraintsEnabled(true)
    }
  }
)
```
