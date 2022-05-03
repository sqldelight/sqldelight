CREATE TABLE myTable(
    data TEXT NOT NULL,
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
