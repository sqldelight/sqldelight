CREATE TABLE table_name(
  column1 TEXT,
  column2 DATE,
  column3 INTEGER
);

SELECT column1, column2
FROM table_name
ORDER BY column1 NULLS FIRST, column2 DESC;

SELECT column1, column2
FROM table_name
ORDER BY column1 DESC NULLS LAST, column2;

SELECT column1, column2, column3
FROM table_name
ORDER BY column1 NULLS FIRST, column2 NULLS LAST, column3;

SELECT
  column1,
  column2,
  column3
FROM
  table_name
ORDER BY
  column1 ASC,
  column2 DESC,
  column3;
