CREATE TABLE myTable(
    data JSON NOT NULL,
    datab JSONB NOT NULL
);

SELECT
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data #- '{"a"}',
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data @> datab,
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data <@ datab,
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data ?? 'b',
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data ??| '{"a","b"}',
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data ??& '{"a"}',
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data @? '$.a[*]',
--error[col 2]: Left side of jsonb expression must be a jsonb column.
  data @@ '$.b[*] > 0'
FROM myTable;

SELECT data ->> 'a', datab -> 'b', data #> '{aa}', datab #>> '{bb}', datab || datab, datab - 'b', datab - 1, datab @@ '$.b[*] > 0'
FROM myTable;
