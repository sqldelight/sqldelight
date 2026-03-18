CREATE TABLE person (
    id INTEGER PRIMARY KEY,
    name TEXT,
    created_at TIMESTAMPTZ
);

SELECT DISTINCT name FROM person;

SELECT DISTINCT id, name FROM person ORDER BY name;

SELECT DISTINCT name FROM person WHERE name LIKE 'A%';

SELECT DISTINCT SUBSTR(name, 1, 1) FROM person;
