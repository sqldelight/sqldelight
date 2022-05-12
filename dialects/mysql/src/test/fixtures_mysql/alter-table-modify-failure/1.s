CREATE TABLE test(
  id VARCHAR(8)
);

ALTER TABLE test
-- error[col 16]: No column found to modify with name id2
  MODIFY COLUMN id2 BIGINT;