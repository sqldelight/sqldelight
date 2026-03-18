CREATE TABLE test (
  -- Using TEXT for the primary key because an INTEGER PRIMARY KEY actually surprisingly
  -- has a default value even without being AUTOINCREMENT.
  _id TEXT NOT NULL PRIMARY KEY,
  column1 TEXT DEFAULT 'sup',
  column2 BLOB NOT NULL,
  column3 REAL
);

CREATE TABLE test2 (
  -- INTEGER NOT NULL PRIMARY KEY should be recognised as having a default value.
  _id INTEGER NOT NULL PRIMARY KEY,
  column1 TEXT DEFAULT 'sup',
  column2 BLOB NOT NULL,
  column3 REAL
);

-- Works fine
INSERT INTO test VALUES (?, ?, ?, ?);

-- Also works fine. Nullable values receive default value of null.
INSERT INTO test (_id, column1, column2) VALUES (?, ?, ?);

-- Works fine, nullable and default value columns are okay.
INSERT INTO test (_id, column2) VALUES (?, ?);

-- Fails as column2 must be specified.
-- error[col 0]: Cannot populate default value for column column2, it must be specified in insert statement.
INSERT INTO test (_id, column1, column3) VALUES (?, ?, ?);

-- Fails as _id must be specified.
-- error[col 0]: Cannot populate default value for column _id, it must be specified in insert statement.
INSERT INTO test(column1, column2, column3) VALUES (?, ?, ?);

-- Fails since not all columns can have default values.
-- error[col 0]: Cannot populate default values for columns (_id, column2), they must be specified in insert statement.
INSERT INTO test DEFAULT VALUES;



-- Works fine
INSERT INTO test2 VALUES (?, ?, ?, ?);

-- Also works fine. Nullable values receive default value of null.
INSERT INTO test2 (_id, column1, column2) VALUES (?, ?, ?);

-- Works fine, nullable and default value columns are okay.
INSERT INTO test2 (_id, column2) VALUES (?, ?);

-- Fails as column2 must be specified.
-- error[col 0]: Cannot populate default value for column column2, it must be specified in insert statement.
INSERT INTO test2 (_id, column1, column3) VALUES (?, ?, ?);

-- Works. An INTEGER PRIMARY KEY has a default value.
INSERT INTO test2(column1, column2, column3) VALUES (?, ?, ?);

-- Fails since not all columns can have default values.
-- error[col 0]: Cannot populate default value for column column2, it must be specified in insert statement.
INSERT INTO test2 DEFAULT VALUES;