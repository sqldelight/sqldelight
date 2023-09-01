CREATE TABLE person (
    id INTEGER PRIMARY KEY,
    name TEXT,
    created_at TIMESTAMPTZ
);

SELECT DISTINCT ON (name) *
FROM person
ORDER BY name, created_at DESC;

SELECT DISTINCT ON (name) *
FROM person
ORDER BY created_at DESC;
