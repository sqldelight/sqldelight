CREATE TABLE U (
  aa TEXT[] NOT NULL,
  bb INTEGER[] NOT NULL
);

CREATE TABLE P (
  a TEXT NOT NULL,
  b INTEGER NOT NULL
);

SELECT UNNEST('{1,2}'::INTEGER[]);

SELECT *
FROM UNNEST('{1,2}'::INTEGER[], '{"foo","bar","baz"}'::TEXT[]);

SELECT UNNEST(aa)
FROM U;

SELECT r.a
FROM U, UNNEST(aa) AS r(a);

SELECT r.a, r.b
FROM U, UNNEST(aa, bb) AS r(a, b);

INSERT INTO P (a, b)
SELECT * FROM UNNEST(?::TEXT[], ?::INTEGER[]) AS i(a, b);

UPDATE P
SET b = u.b
FROM UNNEST(?::TEXT[], ?::INTEGER[]) AS u(a, b)
WHERE P.a = u.a;

DELETE FROM P
WHERE (a, b) IN (
  SELECT *
  FROM UNNEST(?::TEXT[], ?::INTEGER[]) AS d(a, b)
);

SELECT *
FROM U
WHERE EXISTS (
   SELECT 1
   FROM UNNEST(U.aa) AS r(a)
   WHERE LOWER(r.a) LIKE '%' || LOWER('a') || '%');

SELECT DISTINCT b.*
FROM U b
JOIN LATERAL UNNEST(b.aa) AS r(a) ON r.a ILIKE '%' || 'a' || '%';
