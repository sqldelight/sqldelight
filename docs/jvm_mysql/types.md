## MySQL Types

SQLDelight column definitions are identical to regular MySQL column definitions but support an [extra column constraint](#custom-column-types)
which specifies the Kotlin type of the column in the generated interface.

```sql
CREATE TABLE some_types (
  some_tiny_int TINYINT,             -- Retrieved as Int
  some_small_int SMALLINT,           -- Retrieved as Int
  some_medium_int MEDIUMINT,         -- Retrieved as Int
  some_integer INTEGER,              -- Retrieved as Int
  some_int INT,                      -- Retrieved as Int
  some_big_int BIGINT,               -- Retrieved as Long
  some_decimal DECIMAL,              -- Retrieved as Double
  some_dec DEC,                      -- Retrieved as Double
  some_fixed FIXED,                  -- Retrieved as Double
  some_numeric NUMERIC,              -- Retrieved as Double
  some_float FLOAT,                  -- Retrieved as Double
  some_real REAL,                    -- Retrieved as Double
  some_double_prec DOUBLE PRECISION, -- Retrieved as Double
  some_double DOUBLE,                -- Retrieved as Double
  some_date DATE,                    -- Retrieved as String
  some_time TIME,                    -- Retrieved as String
  some_datetime DATETIME,            -- Retrieved as String
  some_timestamp TIMESTAMP,          -- Retrieved as String
  some_year YEAR,                    -- Retrieved as String
  some_char CHAR,                    -- Retrieved as String
  some_varchar VARCHAR(16),          -- Retrieved as String
  some_tiny_text TINYTEXT,           -- Retrieved as String
  some_text TEXT,                    -- Retrieved as String
  some_medium_text MEDIUMTEXT,       -- Retrieved as String
  some_long_text LONGTEXT,           -- Retrieved as String
  some_enum ENUM,                    -- Retrieved as String
  some_set SET,                      -- Retrieved as String
  some_varbinary VARBINARY(8),       -- Retrieved as ByteArray
  some_blob BLOB(8, 8),              -- Retrieved as ByteArray
  some_binary BINARY,                -- Retrieved as ByteArray
  some_json JSON,                    -- Retrieved as String
  some_boolean BOOLEAN,              -- Retrieved as Boolean
);
```

{% include 'common/custom_column_types.md' %}

{% include 'common/types_server_migrations.md' %}