CREATE TABLE test(
    id   SERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE sample(
    id  INT NOT NULL REFERENCES test,
    val INT NOT NULL
);

WITH test_ids0 AS (
    UPDATE test
        SET name = 'Fooey'
        WHERE name = 'Foo'
        RETURNING id AS test_id0
), test_ids3 AS (
    SELECT test_id0 FROM test_ids0
)
INSERT
INTO sample
SELECT test_id0, 100
FROM test_ids0;

WITH insert_test_sample AS (
   INSERT INTO test
   VALUES(1, 'Foo')
   RETURNING id, name
),
insert_sample AS (
    INSERT INTO sample (id, val)
    SELECT id, 31 FROM insert_test_sample
)
SELECT * FROM insert_test_sample;
