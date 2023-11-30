CREATE TABLE test7 (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL
);

INSERT INTO test7 (id, name)
VALUES (1, 'bob')
ON CONFLICT DO NOTHING
;

INSERT INTO test7 (id, name)
VALUES (1, 'bob')
ON CONFLICT (id) DO NOTHING
;

INSERT INTO test7 (id, name)
VALUES (1, 'bob')
ON CONFLICT (name) DO NOTHING
;

INSERT INTO test7 (id, name)
VALUES (1, 'bob')
ON CONFLICT (missing_column) DO NOTHING
;
