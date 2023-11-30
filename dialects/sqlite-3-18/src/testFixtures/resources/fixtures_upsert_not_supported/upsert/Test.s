CREATE TABLE transaction(
    transaction_id INTEGER PRIMARY KEY,
    transaction_uuid TEXT NOT NULL UNIQUE,
    opened_time TEXT NOT NULL,
    finalized_time TEXT,
    count INTEGER
);

INSERT INTO transaction(transaction_uuid, opened_time, finalized_time)
VALUES (?, ?, ?)
ON CONFLICT (transaction_uuid)
DO UPDATE SET opened_time = excluded.opened_time, finalized_time = excluded.finalized_time;