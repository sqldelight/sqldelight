CREATE TABLE myTable(
    data JSON NOT NULL,
    datab JSONB NOT NULL
);

SELECT *
FROM myTable
WHERE
  data -> 'sup' AND
  data ->> 'sup' AND
  data #> 'sup' AND
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data @> 'sup' AND
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data <@ 'sup' AND
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data ? 'sup' AND
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data ?| 'sup' AND
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data ?& 'sup' AND
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data #- 'sup' AND
  datab -> 'sup' AND
  datab ->> 'sup' AND
  datab #> 'sup' AND
  datab @> 'sup' AND
  datab <@ 'sup' AND
  datab ? 'sup' AND
  datab ?| 'sup' AND
  datab ?& 'sup' AND
  datab #- 'sup'
;