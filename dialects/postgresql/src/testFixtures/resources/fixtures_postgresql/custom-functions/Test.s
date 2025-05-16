CREATE TABLE foo (
bar TIMESTAMPTZ NOT NULL
);

SELECT NOW() AS now, date_trunc('hour', bar) AS d, date_part('hour', bar) FROM foo;
