CREATE TABLE test(
  id TEXT NOT NULL,
  name TEXT NOT NULL
);

INSERT INTO test(id, name) VALUES ('id', 'name') RETURNING *;

INSERT INTO test(id, name) VALUES ('id', 'name') RETURNING id;

INSERT INTO test(id, name) VALUES ('id', 'name') RETURNING id, name;

INSERT INTO test(id, name) VALUES ('id', 'name') RETURNING *, id;

INSERT INTO test(id, name) VALUES ('id', 'name') RETURNING id, *;

UPDATE test SET name = 'name' WHERE id = 'id' RETURNING *;

UPDATE test SET name = 'name' WHERE id = 'id' RETURNING id;

UPDATE test SET name = 'name' WHERE id = 'id' RETURNING id, name;

UPDATE test SET name = 'name' WHERE id = 'id' RETURNING *, id;

UPDATE test SET name = 'name' WHERE id = 'id' RETURNING id, *;

DELETE FROM test WHERE id = 'id' RETURNING *;

DELETE FROM test WHERE id = 'id' RETURNING id;

DELETE FROM test WHERE id = 'id' RETURNING id, name;

DELETE FROM test WHERE id = 'id' RETURNING *, id;

DELETE FROM test WHERE id = 'id' RETURNING id, *;
