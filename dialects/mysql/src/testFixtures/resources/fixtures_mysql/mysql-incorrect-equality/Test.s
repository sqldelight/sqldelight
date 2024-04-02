CREATE TABLE test(
  foo TEXT
);

SELECT *
FROM test
-- error[col 6]: Expected '=' but got '=='.
WHERE foo == 'bar';