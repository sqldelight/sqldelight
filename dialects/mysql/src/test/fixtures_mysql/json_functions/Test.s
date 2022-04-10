CREATE TABLE myTable(
    data JSON NOT NULL,
    number INTEGER NOT NULL
);

SELECT *
FROM myTable
WHERE
  data -> 'sup' AND
  data ->> 'sup' AND
--error[col 2]: Left side of json expression must be a json column.
  number -> 'sup' AND
--error[col 2]: Left side of json expression must be a json column.
  number ->> 'sup'
;
