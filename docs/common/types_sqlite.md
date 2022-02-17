## SQLite Types

SQLDelight column definitions are identical to regular SQLite column definitions but support an [extra column constraint](#custom-column-types)
which specifies the Kotlin type of the column in the generated interface.

```sql
CREATE TABLE some_types (
  some_long INTEGER,           -- Stored as INTEGER in db, retrieved as Long
  some_double REAL,            -- Stored as REAL in db, retrieved as Double
  some_string TEXT,            -- Stored as TEXT in db, retrieved as String
  some_blob BLOB,              -- Stored as BLOB in db, retrieved as ByteArray
);
```

## Primitives

A sibling module that adapts primitives for your convenience.

```groovy
dependencies {
  implementation "app.cash.sqldelight:primitive-adapters:{{ versions.sqldelight }}"
}
```

The following adapters exist:

- `BooleanColumnAdapter` — Retrieves `kotlin.Boolean` for an SQL type implicitly stored as `kotlin.Long`
- `FloatColumnAdapter` — Retrieves `kotlin.Float` for an SQL type implicitly stored as `kotlin.Double`
- `IntColumnAdapter` — Retrieves `kotlin.Int` for an SQL type implicitly stored as `kotlin.Long`
- `ShortColumnAdapter` — Retrieves `kotlin.Short` for an SQL type implicitly stored as `kotlin.Long`
