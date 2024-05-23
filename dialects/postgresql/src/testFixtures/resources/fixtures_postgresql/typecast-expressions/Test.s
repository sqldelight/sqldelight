SELECT '1'::text;

SELECT 3.14::text;

SELECT '42'::integer;

SELECT 'true'::boolean;

SELECT concat('tru','e')::boolean;

WITH numbers AS (
  SELECT generate_series(-3.5, 3.5, 1) AS x
)
SELECT x,
  round(x::numeric) AS num_round,
  round(x::double precision) AS dbl_round
FROM numbers;

SELECT '2023-05-01 12:34:56'::TIMESTAMP::DATE;

SELECT '6ba7b810-9dad-11d1-80b4-00c04fd430c8'::UUID;

SELECT '{"a":42}'::JSON;

SELECT '[1,2,3]'::INT[];

SELECT 42::BIGINT;

SELECT 3.14::DOUBLE PRECISION;

SELECT 'f'::BOOLEAN;

SELECT 'hello world'::VARCHAR(5);

SELECT '2023-04-25 10:30:00+02'::TIMESTAMP WITH TIME ZONE;

SELECT '2023-04-25 10:30:00+02'::TIMESTAMP::DATE;

SELECT ?::INT;
