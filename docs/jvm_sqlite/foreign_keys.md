{% if multiplatform %}
## JVM SQLite
{% else %}
# Foreign Keys
{% endif %}
You can enable foreign key constraints for the JVM SQLite driver by passing the setting to the driver's properties.

```kotlin
JdbcSqliteDriver(
  url = "...", 
  properties = Properties().apply { put("foreign_keys", "true") }
)
```
