## PostgreSQL Types

SQLDelight column definitions are identical to regular PostgreSQL column definitions but support an
[extra column constraint](#custom-column-types) which specifies the Kotlin type of the column in the
generated interface.

```sql
CREATE TABLE some_types (
  some_smallint SMALLINT,               -- Retrieved as Short
  some_int2 INT2,                       -- Retrieved as Short
  some_integer INTEGER,                 -- Retrieved as Int
  some_int INT,                         -- Retrieved as Int
  some_int4 INT4,                       -- Retrieved as Int
  some_bigint BIGINT,                   -- Retrieved as Long
  some_int8 INT8,                       -- Retrieved as Long
  some_numeric NUMERIC,                 -- Retrieved as BigDecimal
  some_decimal DECIMAL,                 -- Retrieved as Double
  some_real REAL,                       -- Retrieved as Double
  some_float4 FLOAT4,                   -- Retrieved as Double
  some_double_prec DOUBLE PRECISION,    -- Retrieved as Double
  some_float8 FLOAT8,                   -- Retrieved as Double
  some_smallserial SMALLSERIAL,         -- Retrieved as Short
  some_serial2 SERIAL2,                 -- Retrieved as Short
  some_serial SERIAL,                   -- Retrieved as Int
  some_serial4 SERIAL4,                 -- Retrieved as Int
  some_bigserial BIGSERIAL,             -- Retrieved as Long
  some_serial8 SERIAL8,                 -- Retrieved as Long
  some_character CHARACTER,             -- Retrieved as String
  some_char CHAR,                       -- Retrieved as String
  some_char_var CHARACTER VARYING(16),  -- Retrieved as String
  some_varchar VARCHAR(16),             -- Retrieved as String
  some_text TEXT,                       -- Retrieved as String
  some_date DATE,                       -- Retrieved as LocalDate
  some_time TIME,                       -- Retrieved as LocalTime
  some_timestamp TIMESTAMP,             -- Retrieved as LocalDateTime
  some_timestamp TIMESTAMPTZ,           -- Retrieved as OffsetDateTime
  some_json JSON,                       -- Retrieved as String
  some_jsonb JSONB,                     -- Retrieved as String
  some_interval INTERVAL,               -- Retrieved as PGInterval
  some_uuid UUID                        -- Retrieved as UUID
  some_bool BOOL,                       -- Retrieved as Boolean
  some_boolean BOOLEAN,                 -- Retrieved as Boolean
  some_bytea BYTEA                      -- Retrieved as ByteArray
);
```

{% include 'common/custom_column_types.md' %}

{% include 'common/types_server_migrations.md' %}