CREATE TABLE test8 (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL
);

INSERT INTO test8 (id, name)
VALUES (1, 'bob')
ON CONFLICT DO UPDATE SET name = name
;

INSERT INTO test8 (id, name)
VALUES (1, 'bob')
ON CONFLICT (id) DO UPDATE SET name = name
;

INSERT INTO test8 (id, name)
VALUES (1, 'bob')
ON CONFLICT (id) DO UPDATE SET name = excluded.name
;

INSERT INTO test8 (id, name)
VALUES (1, 'bob')
ON CONFLICT (id, name) DO UPDATE SET name = DEFAULT
;

INSERT INTO test8 (id, name)
VALUES (1, 'bob')
ON CONFLICT (id, name) DO UPDATE SET name = 'new' || name
;
