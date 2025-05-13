CREATE TABLE test4 (
  value TEXT NOT NULL,
  otherValue TEXT NOT NULL
);

CREATE INDEX test4IndexValue ON test4(value);

-- error[col 30]: Cannot drop indexed column "value" ("test4IndexValue")
ALTER TABLE test4 DROP COLUMN value;

CREATE TABLE test5 (
  value TEXT NOT NULL,
  otherValue TEXT NOT NULL
);

ALTER TABLE test5 DROP COLUMN value;
