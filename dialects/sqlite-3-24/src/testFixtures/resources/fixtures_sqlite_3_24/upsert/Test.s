CREATE TABLE test(
    test_id INTEGER PRIMARY KEY,
    test_uuid TEXT NOT NULL UNIQUE,
    opened_time TEXT NOT NULL,
    finalized_time TEXT,
    count INTEGER
);

INSERT INTO test(test_uuid, opened_time, finalized_time)
VALUES (?, ?, ?)
ON CONFLICT (test_uuid)
DO UPDATE SET opened_time = excluded.opened_time, finalized_time = excluded.finalized_time;

INSERT INTO test(test_uuid, opened_time, finalized_time)
VALUES (?, ?, ?)
ON CONFLICT (test_uuid)
DO UPDATE SET finalized_time = excluded.finalized_time
WHERE finalized_time IS NULL;

INSERT INTO test(test_uuid, opened_time, finalized_time)
VALUES (?, ?, ?)
ON CONFLICT (test_uuid) DO NOTHING;

INSERT INTO test(test_uuid, opened_time, finalized_time)
VALUES (?, ?, ?)
ON CONFLICT DO NOTHING;

-- SET clause should be able to access CTE
WITH t(foo) AS (VALUES (?))
INSERT INTO test(test_uuid, opened_time, finalized_time)
VALUES (?, ?, ?)
ON CONFLICT (test_uuid)
DO UPDATE SET opened_time = excluded.opened_time, finalized_time = (SELECT foo FROM t);

-- Conflict list should only be able to access the test test, not the CTE
WITH t(foo) AS (VALUES(?))
INSERT INTO test(test_uuid, opened_time, finalized_time)
VALUES (?, ?, ?)
ON CONFLICT (foo) DO NOTHING;

-- If specifying a conflict resolution strategy like REPLACE, the ON CONFLICT must be DO NOTHING
INSERT OR REPLACE INTO test(test_uuid, opened_time, finalized_time)
VALUES (?, ?, ?)
ON CONFLICT (test_uuid) DO NOTHING;

-- Cannot use DO UPDATE when OR REPLACE conflict resolution algorithm was specified
INSERT OR REPLACE INTO test(test_uuid, opened_time, finalized_time)
VALUES (?, ?, ?)
ON CONFLICT (test_uuid) DO UPDATE SET opened_time = excluded.opened_time;

-- Should pass
INSERT INTO test(test_uuid, opened_time, finalized_time, count)
VALUES (?, ?, ?, ?)
ON CONFLICT (test_uuid) DO UPDATE SET count = test.count + excluded.count;

-- Should fail
INSERT INTO test(test_uuid, opened_time, finalized_time, count)
VALUES (?, ?, ?, ?)
ON CONFLICT (test_uuid) DO UPDATE SET count = test_alias.count + excluded.count;

-- Should fail
INSERT INTO test AS test_alias(test_uuid, opened_time, finalized_time, count)
VALUES (?, ?, ?, ?)
ON CONFLICT (test_uuid) DO UPDATE SET count = test.count + excluded.count;

-- Should pass
INSERT INTO test AS test_alias(test_uuid, opened_time, finalized_time, count)
VALUES (?, ?, ?, ?)
ON CONFLICT (test_uuid) DO UPDATE SET count = test_alias.count + excluded.count;