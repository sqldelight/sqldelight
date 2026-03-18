{% if multiplatform %}
## Native SQLite
{% else %}
# Foreign Keys
{% endif %}

You can enable foreign key constraints for the Native SQLite driver by enabling them in the database configuration.

```kotlin
NativeSqliteDriver(
  schema = Database.Schema,
  onConfiguration = { config: DatabaseConfiguration ->
    config.copy(
      extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true)
    )
  }
)
```
