CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  name TEXT,
  active INTEGER,
  created_at TEXT
);

SELECT GROUP_CONCAT(name, ', ' ORDER BY created_at)
FROM users;

SELECT GROUP_CONCAT(name, ', ' ORDER BY created_at ASC NULLS FIRST)
FROM users;

SELECT GROUP_CONCAT(name, ', ' ORDER BY created_at DESC NULLS LAST)
FROM users;

SELECT GROUP_CONCAT(DISTINCT name ORDER BY created_at)
FROM users;

SELECT GROUP_CONCAT(name, '' ORDER BY
    name COLLATE NOCASE, id
  ) AS someNames
FROM users;

SELECT GROUP_CONCAT(name, ', ' ORDER BY name)
   FILTER (WHERE active = 1)
FROM users;

-- string_agg is an alias for group_concat in SQLite
SELECT STRING_AGG(name, ', ' ORDER BY created_at DESC)
FROM users;


-- The separator is any expression, not only a string literal
SELECT GROUP_CONCAT(name, created_at)
FROM users;

SELECT STRING_AGG(name, created_at)
FROM users;

SELECT STRING_AGG(name, created_at ORDER BY name)
FROM users;

SELECT STRING_AGG(name, ', ' || ' ' ORDER BY name)
FROM users;

SELECT STRING_AGG(name, created_at ORDER BY name)
   FILTER (WHERE active = 1)
FROM users;
