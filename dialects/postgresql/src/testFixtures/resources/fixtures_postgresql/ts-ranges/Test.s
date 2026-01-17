CREATE TABLE Schedules(
  slot TSTZRANGE NOT NULL CHECK(
      date_part('minute', LOWER(slot)) IN (00, 30)
      AND
      date_part('minute', UPPER(slot)) IN (00, 30)),
    duration INT GENERATED ALWAYS AS (
      EXTRACT (epoch FROM UPPER(slot) - LOWER(slot))/60
  ) STORED CHECK(duration IN (30, 60, 90, 120)),
  EXCLUDE USING GIST(slot WITH &&)
);

CREATE TABLE Reservations (
  room TEXT,
  during TSTZRANGE,
  CONSTRAINT no_rooms_overlap EXCLUDE USING GIST (room WITH =, during WITH &&)
);

CREATE TABLE Ranges (
  id INTEGER,
  ts_1 TSRANGE,
  ts_2 TSRANGE,
  tst_1 TSTZRANGE,
  tst_2 TSTZRANGE,
  tsm_1 TSMULTIRANGE,
  tsm_2 TSMULTIRANGE,
  tstm_1 TSTZMULTIRANGE,
  tstm_2 TSTZMULTIRANGE
);

SELECT CURRENT_TIMESTAMP + INTERVAL '1 day' <@ tstzmultirange(
  tstzrange(CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 day' ),
  tstzrange(CURRENT_TIMESTAMP + INTERVAL '3 day' , CURRENT_TIMESTAMP + INTERVAL '6 day')
);

SELECT *
FROM Ranges
WHERE ts_1 <@ ts_2;

SELECT ts_2 @> ts_1, tst_2 @> tst_1, tsm_2 @> tsm_1, tstm_2 @> tstm_1,
ts_2 && ts_1, tst_2 && tst_1, tsm_2 && tsm_1, tstm_2 && tstm_1
FROM Ranges;

SELECT ts_1 && ts_2, ts_1 << ts_2, ts_1 >> ts_2, ts_1 &> ts_2, ts_1 &< ts_2, ts_1 -|- ts_2, ts_1 * ts_2, ts_1 + ts_2, ts_1 - ts_2,
tst_1 && tst_2, tst_1 << tst_2, tst_1 >> tst_2, tst_1 &> tst_2, tst_1 &< tst_2, tst_1 -|- tst_2, tst_1 * tst_2, tst_1 + tst_2, tst_1 - tst_2,
tsm_1 && tsm_2, tsm_1 << tsm_2, tsm_1 >> tsm_2, tsm_1 &> tsm_2, tsm_1 &< tsm_2, tsm_1 -|- tsm_2, tsm_1 * tsm_2, tsm_1 + tsm_2, tsm_1 - tsm_2,
tstm_1 && tstm_2, tstm_1 << tstm_2, tstm_1 >> tstm_2, tstm_1 &> tstm_2, tstm_1 &< tstm_2, tstm_1 -|- tstm_2, tstm_1 * tstm_2, tstm_1 + tstm_2, tstm_1 - tstm_2
FROM Ranges;

SELECT datemultirange(tsrange('2021-06-01', '2021-06-30', '[]')) - range_agg(during) AS availability
FROM Reservations
WHERE during && tsrange('2021-06-01', '2021-06-30', '[]');

SELECT tstzmultirange(tstzrange('2010-01-01 14:30:00', '2010-01-01 15:30:00', '[]')) - range_agg(tst_1)
FROM Ranges
WHERE tst_2 && tstzrange('2010-01-01 14:30:00', '2010-01-01 15:30:00', '[]');

--error[col 7]: expression must be ARRAY, JSONB, TSVECTOR, TSRANGE, TSTZRANGE, TSMULTIRANGE, TSTZMULTIRANGE.
SELECT id @> ts_1
FROM Ranges;
