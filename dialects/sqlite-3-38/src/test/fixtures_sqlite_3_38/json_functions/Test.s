CREATE TABLE myTable(
    data TEXT NOT NULL,
    otherData TEXT,
    number INTEGER NOT NULL
);

SELECT *
FROM myTable
WHERE
  data -> 'sup' AND
  data ->> 'sup' AND
--error[col 2]: Left side of json expression must be a text column.
  number -> 'sup' AND
--error[col 2]: Left side of json expression must be a text column.
  number ->> 'sup'
;

UPDATE myTable
SET otherData = subquery.data -> 'sup'
FROM (SELECT number, data FROM myTable) subquery
WHERE subquery.number = myTable.number;

UPDATE myTable
SET data = data -> newData
FROM(
  SELECT 'sup' AS newData
)
WHERE data = '';
