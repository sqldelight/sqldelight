CREATE TABLE test(
  id VARCHAR(8)
);

ALTER TABLE test
  RENAME TO new_test,
  ADD COLUMN id2 VARCHAR(8),
  ADD COLUMN id3 VARCHAR(8);

SELECT id, id2, id3
FROM new_test;