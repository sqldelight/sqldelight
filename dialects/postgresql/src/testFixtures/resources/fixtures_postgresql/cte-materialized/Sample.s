CREATE TABLE test(
    id   SERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE sample(
    id  INT NOT NULL REFERENCES test,
    val INT NOT NULL
);

--- If a WITH query is non-recursive and side-effect-free (that is, it is a SELECT containing no volatile functions)
--- then it can be folded into the parent query, allowing joint optimization of the two query levels.
--- By default, this happens if the parent query references the WITH query just once, but not if it references the WITH
--- query more than once.
--- You can override that decision by specifying MATERIALIZED to force separate calculation of the WITH query
WITH test_ids AS MATERIALIZED (
    SELECT id FROM test
)
SELECT id FROM test_ids;

--- or by specifying NOT MATERIALIZED to force it to be merged into the parent query.
--- The latter choice risks duplicate computation of the WITH query, but it can still give a net savings if
--- each usage of the WITH query needs only a small part of the WITH query's full output.
WITH test_ids AS NOT MATERIALIZED (
    INSERT INTO test (name)
        VALUES ('Foo')
        RETURNING id AS test_id
)
INSERT
INTO sample
SELECT test_id, 100
FROM test_ids;

WITH test_ids AS MATERIALIZED (
    UPDATE test
        SET name = 'Fooey'
        WHERE name = 'Foo'
        RETURNING id AS test_id
)
INSERT
INTO sample
SELECT test_id, 100
FROM test_ids;

WITH sample_ids AS NOT MATERIALIZED(
    DELETE FROM sample
        WHERE val < 100
        RETURNING id AS sample_id
)
SELECT sample_id FROM sample_ids;

WITH deleted_test_sample AS MATERIALIZED (
  DELETE FROM test
  RETURNING id
), deleted_sample AS NOT MATERIALIZED (
  DELETE FROM sample WHERE id IN (SELECT id FROM deleted_test_sample)
  RETURNING id
)
SELECT * FROM deleted_test_sample;
