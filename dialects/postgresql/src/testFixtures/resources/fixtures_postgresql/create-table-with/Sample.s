CREATE TABLE Write_Heavy (
   id    BIGINT PRIMARY KEY,
   value TEXT
) WITH (
    fillfactor          = 70,   -- reserve 30% of each page for HOT updates
    autovacuum_enabled  = TRUE,
    autovacuum_vacuum_scale_factor = 0.01
  );

CREATE TABLE Rides(
  vendor_id TEXT,
  pickup_datetime TIMESTAMP WITHOUT TIME ZONE NOT NULL
) WITH (
    tsdb.hypertable,
    tsdb.create_default_indexes=FALSE,
    tsdb.segmentby='vendor_id',
    tsdb.orderby='pickup_datetime DESC'
  );

CREATE UNLOGGED TABLE Session_Cache (
    session_key  TEXT        PRIMARY KEY,
    payload      JSONB       NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL
) WITH (fillfactor = 70);

CREATE TEMP TABLE Staging_Imports (
    raw_line    TEXT,
    imported_at TIMESTAMPTZ DEFAULT NOW()
) WITH (fillfactor = 70);
