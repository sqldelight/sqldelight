CREATE TABLE test (
  id INTEGER PRIMARY KEY,
  value INTEGER,
  value2 INTEGER
);

INSERT INTO test
(id, value, value2)
VALUES (1, 2, 3)
ON DUPLICATE KEY UPDATE
value = VALUES(value),
value2 = value2 + 1;
