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

SELECT data -> 'a' -> 'b', datab -> 'a' -> 'b'
FROM myTable;

SELECT t
FROM myTable
WHERE t = :txt
  AND data -> 'a' @> :a
  AND datab -> 'aa' -> 'bb' ?? :bb;

SELECT
    data -> 'a' -> 'b',
    data -> 'a' ->> 'b',
    data #> '{a}' -> 'b',
    data #>> '{a}',
    datab - '{a}' -> 'a'
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

SELECT jsonb_agg(myTable) FILTER (WHERE (datab->>'in_stock')::BOOLEAN) FROM myTable;

SELECT jsonb_agg(datab->'color') FILTER (WHERE datab ?? 'color') AS colors
FROM myTable;

SELECT jsonb_object_agg(datab->>'color', datab)
FROM myTable;

SELECT jsonb_object_agg(t, datab) FILTER (WHERE t IS NOT NULL)
FROM myTable;

SELECT json_object_agg_unique(t, data)
FROM myTable;

SELECT jsonb_object_agg_strict(t, datab)
FROM myTable;

SELECT jsonb_object_agg_strict(t, datab ORDER BY t DESC)
FROM myTable;
