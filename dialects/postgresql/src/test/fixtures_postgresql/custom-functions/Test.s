CREATE TABLE foo (
bar TIMESTAMPTZ NOT NULL
);

SELECT NOW() AS now, date_trunc('hour', bar) AS d FROM foo;
