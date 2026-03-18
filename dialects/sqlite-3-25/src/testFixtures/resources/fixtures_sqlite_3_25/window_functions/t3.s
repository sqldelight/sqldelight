CREATE TABLE numbers(
  value INTEGER NOT NULL
);

SELECT value
FROM (
  SELECT
   value,
    CASE
      WHEN ((row_number() OVER(ORDER BY value ASC) - 1) % :limit) = 0 THEN 1
      WHEN value = :anchor THEN 1
      ELSE 0
    END page_boundary
  FROM numbers
  ORDER BY value ASC
)
WHERE page_boundary = 1;
