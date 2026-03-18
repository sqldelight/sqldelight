CREATE TABLE run(
  status TEXT NOT NULL
);

CREATE TABLE example_table(
  id INTEGER NOT NULL
);

SELECT * FROM run WHERE status = 'WAITING' LIMIT 1 FOR UPDATE;

SELECT * FROM run WHERE status = 'WAITING' LIMIT 1 FOR UPDATE SKIP LOCKED;

SELECT * FROM run WHERE status = 'WAITING' LIMIT 1 FOR SHARE NOWAIT;

SELECT * FROM run
  JOIN example_table ex ON ex.id = 1
  WHERE status = 'WAITING'
  LIMIT 1
  FOR NO KEY UPDATE OF ex SKIP LOCKED;

SELECT * FROM run
  JOIN example_table ON example_table.id = 1
  WHERE status = 'WAITING'
  LIMIT 1
  FOR KEY SHARE OF example_table NOWAIT
  FOR UPDATE OF run SKIP LOCKED;

SELECT * FROM run
  JOIN example_table ON example_table.id = 1
  WHERE status = 'WAITING'
  LIMIT 1
  FOR UPDATE OF run, example_table NOWAIT;

SELECT * FROM (SELECT * FROM example_table FOR UPDATE) e WHERE id = 5;
