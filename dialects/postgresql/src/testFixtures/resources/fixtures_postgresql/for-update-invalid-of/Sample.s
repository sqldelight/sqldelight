CREATE TABLE run(
  status TEXT NOT NULL
);

CREATE TABLE example_table(
  id INTEGER NOT NULL
);

SELECT * FROM run WHERE status = 'WAITING' LIMIT 1 FOR UPDATE OF example_table;

SELECT * FROM run
  JOIN example_table ex ON ex.id = 1
  WHERE status = 'WAITING'
  LIMIT 1
  FOR UPDATE OF example_table;
