CREATE TABLE test (
  id INTEGER,
  id_stored INTEGER AS (id * 2) STORED,
  id_virtual INTEGER AS (id * 2) VIRTUAL,
  id_no_storage INTEGER AS (id * 2),
  id_bigger_than_ten INTEGER GENERATED ALWAYS AS (id > 10) NOT NULL
);
