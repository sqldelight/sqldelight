## PostgreSQL Types

SQLDelight column definitions are identical to regular PostgreSQL column definitions but support an
[extra column constraint](#custom-column-types) which specifies the Kotlin type of the column in the
generated interface.

```sql
CREATE TABLE some_types (
  some_smallint SMALLINT,               -- Retrieved as Int
  some_int2 INT2,                       -- Retrieved as Int
  some_integer INTEGER,                 -- Retrieved as Int
  some_int INT,                         -- Retrieved as Int
  some_int4 INT4,                       -- Retrieved as Int
  some_bigint BIGINT,                   -- Retrieved as Long
  some_int8 INT8,                       -- Retrieved as Long
  some_numeric NUMERIC,                 -- Retrieved as Int
  some_decimal DECIMAL,                 -- Retrieved as Int
  some_real REAL,                       -- Retrieved as Double
  some_float4 FLOAT4,                   -- Retrieved as Double
  some_double_prec DOUBLE PRECISION,    -- Retrieved as Double
  some_float8 FLOAT8,                   -- Retrieved as Double
  some_character CHARACTER,             -- Retrieved as String
  some_char CHAR,                       -- Retrieved as String
  some_char_var CHARACTER VARYING(16),  -- Retrieved as String
  some_varchar VARCHAR(16),             -- Retrieved as String
  some_text TEXT,                       -- Retrieved as String
  some_date DATE,                       -- Retrieved as String
  some_time TIME,                       -- Retrieved as String
  some_timestamp TIMESTAMP,             -- Retrieved as String
  some_json JSON                        -- Retrieved as String
);
```

{% include 'common/custom_column_types.md' %}

{% include 'common/types_server_migrations.md' %}