SELECT g
FROM generate_series(1, 5) AS g;

SELECT g
FROM generate_series(0, 10, 5) AS g;

SELECT s.a AS dates
FROM generate_series(0, 14, 7) AS s(a);

SELECT *
FROM generate_series(?::INTEGER, ?::INTEGER);

SELECT g.t
FROM generate_series('2024-03-29 00:00:00'::TIMESTAMP, '2024-03-29 03:00:00'::TIMESTAMP, '1 hour'::INTERVAL) AS g(t);
