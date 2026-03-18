CREATE TABLE test(
    id   SERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE sample(
    id  INT NOT NULL REFERENCES test,
    val INT NOT NULL
);

WITH test_ids AS (
    SELECT id FROM test
)
SELECT id FROM test_ids;

WITH test_ids AS (
    INSERT INTO test (name)
        VALUES ('Foo')
        RETURNING id AS test_id
)
INSERT
INTO sample
SELECT test_id, 100
FROM test_ids;

WITH test_ids AS (
    INSERT INTO test (name)
        VALUES ('Foo')
        RETURNING id AS test_id
)
INSERT
INTO sample
SELECT test_id, 100
FROM test_ids;

WITH test_ids AS (
    UPDATE test
        SET name = 'Fooey'
        WHERE name = 'Foo'
        RETURNING id AS test_id
)
INSERT
INTO sample
SELECT test_id, 100
FROM test_ids;

WITH sample_ids AS (
    DELETE FROM sample
        WHERE val < 100
        RETURNING id AS sample_id
)
SELECT sample_id FROM sample_ids;
