CREATE TABLE test (
  value TEXT NOT NULL
);

ALTER TABLE test
  RENAME COLUMN value TO value2;

-- error[col 7]: No column found with name value
SELECT value
FROM test;

SELECT value2
FROM test;

ALTER TABLE test
  RENAME COLUMN value2 TO value3;

-- error[col 7]: No column found with name value2
SELECT value2
FROM test;

SELECT value3
FROM test;

