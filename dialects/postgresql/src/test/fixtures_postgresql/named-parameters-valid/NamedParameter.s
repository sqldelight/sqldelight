CREATE TABLE test (
  _id INTEGER NOT NULL,
  name VARCHAR(100) NOT NULL
);

SELECT *
FROM test
WHERE _id = :id AND name = ?;
