CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  name TEXT
);

-- Sqlite allows DISTINCT only on an aggregate with a single argument
SELECT GROUP_CONCAT(DISTINCT name)
FROM users;

SELECT GROUP_CONCAT(DISTINCT name ORDER BY name)
FROM users;

-- error[col 7]: DISTINCT aggregates must have exactly one argument
SELECT GROUP_CONCAT(DISTINCT name, ', ')
FROM users;

-- error[col 7]: DISTINCT aggregates must have exactly one argument
SELECT GROUP_CONCAT(DISTINCT name, ', ' ORDER BY name)
FROM users;

-- error[col 7]: DISTINCT aggregates must have exactly one argument
SELECT STRING_AGG(DISTINCT name, ', ')
FROM users;

-- string_agg always requires the separator argument
-- error[col 7]: Wrong number of arguments to function string_agg()
SELECT STRING_AGG(name)
FROM users;

-- error[col 7]: Wrong number of arguments to function string_agg()
SELECT STRING_AGG(DISTINCT name)
FROM users;

-- error[col 7]: DISTINCT aggregates must have exactly one argument
SELECT STRING_AGG(DISTINCT name, name)
FROM users;
