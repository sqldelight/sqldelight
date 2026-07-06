CREATE TABLE test(
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL
);

UPDATE test
SET name = REPLACE(name, 'foo', 'bar');
