CREATE TABLE test3 (
  value TEXT UNIQUE NOT NULL,
  otherValue TEXT NOT NULL
);

-- error[col 30]: Cannot drop UNIQUE column "value"
ALTER TABLE test3 DROP COLUMN value;
