CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT
);

SELECT id
FROM (
  SELECT
    id,
    CASE
      WHEN ((row_number() OVER(ORDER BY id ASC) - 0) % 5) = 0 THEN 1
      WHEN id = 5 THEN 1
      ELSE 0
    END AS page_boundary
    FROM player
    ORDER BY id ASC
)
WHERE page_boundary = 1;
