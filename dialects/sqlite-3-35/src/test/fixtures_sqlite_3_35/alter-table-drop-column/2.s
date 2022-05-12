CREATE TABLE test2 (
  value TEXT PRIMARY KEY NOT NULL,
  otherValue TEXT NOT NULL
);

-- error[col 30]: Cannot drop PRIMARY KEY column "value"
ALTER TABLE test2 DROP COLUMN value;
