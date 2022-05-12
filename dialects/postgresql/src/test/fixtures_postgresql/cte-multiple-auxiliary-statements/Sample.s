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
