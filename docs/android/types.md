# Types

SQLDelight column definitions are identical to regular SQLite column definitions but support an extra column constraint
which specifies the Kotlin type of the column in the generated interface. SQLDelight natively supports Long, Double, String, ByteArray, Int, Short, Float, and Booleans.

```sql
CREATE TABLE some_types (
  some_long INTEGER,           -- Stored as INTEGER in db, retrieved as Long
  some_double REAL,            -- Stored as REAL in db, retrieved as Double
  some_string TEXT,            -- Stored as TEXT in db, retrieved as String
  some_blob BLOB,              -- Stored as BLOB in db, retrieved as ByteArray
  some_int INTEGER AS Int,     -- Stored as INTEGER in db, retrieved as Int
  some_short INTEGER AS Short, -- Stored as INTEGER in db, retrieved as Short
  some_float REAL AS Float     -- Stored as REAL in db, retrieved as Float
);
```

Boolean columns are stored in the db as `INTEGER`, and so they can be given `INTEGER` column constraints. Use `DEFAULT 0` to default to false, for example.

```sql
CREATE TABLE hockey_player (
  injured INTEGER AS Boolean DEFAULT 0
)
```