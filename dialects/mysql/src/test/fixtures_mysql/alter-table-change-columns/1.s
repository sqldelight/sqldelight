CREATE TABLE test(
  id VARCHAR(8)
);

ALTER TABLE test
  RENAME TO new_test,
  CHANGE COLUMN id id2 VARCHAR(8);

SELECT id2
FROM new_test;