CREATE TABLE person (
    id INTEGER PRIMARY KEY,
    name TEXT,
    created_at TIMESTAMPTZ
);

SELECT DISTINCT ON (name) *
FROM person;

SELECT DISTINCT ON (name) *
FROM person
ORDER BY name, created_at DESC;

SELECT DISTINCT ON (id, name) id, name
FROM person
ORDER BY name DESC;

SELECT DISTINCT ON (name, id) id, name, created_at
FROM person
ORDER BY id DESC;

SELECT DISTINCT ON (name, id) id, name
FROM person
ORDER BY id, name ASC;

SELECT DISTINCT ON (name, id) id, name
FROM person
ORDER BY id, name, created_at ASC;

-- fail
SELECT DISTINCT ON (name) *
FROM person
ORDER BY created_at DESC;

-- fail
SELECT DISTINCT ON (name, created_at) id, name, created_at
FROM person
ORDER BY id, name, created_at DESC;

-- fail
SELECT DISTINCT ON (name, id) id, name, created_at
FROM person
ORDER BY name, created_at, id DESC;
