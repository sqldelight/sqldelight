## MySQL Types

SQLDelight column definitions are identical to regular H2 column definitions but support an
[extra column constraint](#custom-column-types) which specifies the Kotlin type of the column in the
generated interface.

```sql
CREATE TABLE some_types (
  some_tiny_int TINYINT,                           -- Retrieved as Byte
  some_small_int SMALLINT,                         -- Retrieved as Short
  some_integer INTEGER,                            -- Retrieved as Int
  some_int INT,                                    -- Retrieved as Int
  some_big_int BIGINT,                             -- Retrieved as Long
  some_decimal DECIMAL(6,5),                       -- Retrieved as Int
  some_dec DEC(6,5),                               -- Retrieved as Int
  some_numeric NUMERIC(6,5),                       -- Retrieved as Int
  some_float FLOAT(6),                             -- Retrieved as Double
  some_real REAL,                                  -- Retrieved as Double
  some_double DOUBLE,                              -- Retrieved as Double
  some_double_precision DOUBLE PRECISION,          -- Retrieved as Double
  some_boolean BOOLEAN,                            -- Retrieved as Boolean
  some_date DATE,                                  -- Retrieved as String
  some_time TIME,                                  -- Retrieved as String
  some_timestamp2 TIMESTAMP(6),                    -- Retrieved as String
  some_char CHAR,                                  -- Retrieved as String
  some_character CHARACTER(6),                     -- Retrieved as String
  some_char_varying CHAR VARYING(6),               -- Retrieved as String
  some_longvarchar LONGVARCHAR,                    -- Retrieved as String
  some_character_varying CHARACTER VARYING(6),     -- Retrieved as String
  some_varchar VARCHAR(16),                        -- Retrieved as String
  some_clo CHARACTER LARGE OBJECT(16),             -- Retrieved as String
  some_clob clob(16 M CHARACTERS),                 -- Retrieved as String
  some_binary BINARY,                              -- Retrieved as ByteArray
  some_binary2 BINARY(6),                          -- Retrieved as ByteArray
  some_longvarbinary LONGVARBINARY,                -- Retrieved as ByteArray
  some_longvarbinary2 LONGVARBINARY(6),            -- Retrieved as ByteArray
  some_binary_varying BINARY VARYING(6),           -- Retrieved as ByteArray
  some_varbinary VARBINARY(8),                     -- Retrieved as ByteArray
  some_uuid UUID,                                  -- Retrieved as ByteArray
  some_blob BLOB,                                  -- Retrieved as ByteArray
  some_blo BINARY LARGE OBJECT(6),                 -- Retrieved as ByteArray
  some_bit BIT,                                    -- Retrieved as ByteArray
  some_bit2 BIT(6),                                -- Retrieved as ByteArray
  some_bit_varying BIT VARYING(6),                 -- Retrieved as ByteArray
  some_interval INTERVAL YEAR TO MONTH,            -- Retrieved as ByteArray
  some_interval2 INTERVAL YEAR(3),                 -- Retrieved as ByteArray
  some_interval3 INTERVAL DAY(4) TO HOUR,          -- Retrieved as ByteArray
  some_interval4 INTERVAL MINUTE(4) TO SECOND(6),  -- Retrieved as ByteArray
  some_interval5 INTERVAL SECOND(4,6)              -- Retrieved as ByteArray
);
```

{% include 'common/custom_column_types.md' %}

{% include 'common/types_server_migrations.md' %}