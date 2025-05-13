CREATE TABLE myTable(
    data JSON NOT NULL,
    datab JSONB NOT NULL,
    t TEXT NOT NULL
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

SELECT row_to_json(myTable) FROM myTable;

SELECT json_agg(myTable) FROM myTable;

SELECT to_json(myTable) FROM myTable;

SELECT to_jsonb(myTable.t) FROM myTable;

WITH myTable_cte AS (
  SELECT t FROM myTable
)
SELECT row_to_json(myTable_cte) FROM myTable_cte;

SELECT to_jsonb('Hello World'::text);

SELECT row_to_json(r)
FROM (
  SELECT t FROM myTable
) r;
