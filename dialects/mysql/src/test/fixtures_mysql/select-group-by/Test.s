CREATE TABLE test(
  someColumn VARCHAR(8) NOT NULL,
  someColumn2 VARCHAR(8) NOT NULL
);

SELECT someColumn
FROM test
GROUP BY someColumn;

SELECT someColumn
FROM test
GROUP BY COALESCE(someColumn, 'default');
