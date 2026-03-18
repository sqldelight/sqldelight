CREATE TABLE test (
  id INTEGER,
  id_bigger_than_ten INTEGER GENERATED ALWAYS AS (id > 10) NOT NULL
);
