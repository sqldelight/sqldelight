CREATE TABLE t1(
   id INTEGER PRIMARY KEY -- inserting NULL is allowed for INTEGER PRIMARY KEY because STRICT preserves autoincrement NULL-to-rowid
) STRICT;

CREATE TABLE t2(
  id TEXT PRIMARY KEY -- inserting NULL is not allowed with STRICT
) STRICT;

CREATE TABLE t3(
  id INTEGER,
  txt TEXT,
  PRIMARY KEY (id, txt) -- NULLs are not allowed with STRICT on composite primary key fields
) STRICT;

CREATE TABLE t4(
   id INTEGER,
   txt TEXT,
   PRIMARY KEY (id, txt) -- NULLs are allowed without STRICT
);
