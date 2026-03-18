CREATE TABLE myTable(
  foo REAL NOT NULL,
  bar NUMERIC NOT NULL
);

SELECT
corr(foo),
stddev(bar),
stddev(foo),
regr_count(foo)
FROM myTable GROUP BY foo, bar;
