CREATE TABLE test(
  id VARCHAR(8),
  to_drop VARCHAR(8)
);

ALTER TABLE test
  DROP COLUMN to_drop;

ALTER TABLE test
-- error[col 14]: No column found with name to_drop
  DROP COLUMN to_drop;

ALTER TABLE test
  DROP COLUMN IF EXISTS to_drop;

ALTER TABLE test
  DROP COLUMN IF EXISTS id;

ALTER TABLE test
  DROP COLUMN IF EXISTS xyz;
