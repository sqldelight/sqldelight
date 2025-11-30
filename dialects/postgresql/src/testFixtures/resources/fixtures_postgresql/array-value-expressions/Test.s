SELECT ARRAY[1, 2.0::INT]::BIGINT;

SELECT ARRAY[1::BIGINT, 2.5::INT, 3];

SELECT ARRAY['a', 'b'::TEXT, UPPER('c')];

WITH series AS (
  SELECT generate_series(1, 10)
)
SELECT ARRAY[random()::REAL, random()::REAL, random()::REAL]
FROM series;
