CREATE TABLE data (
  id INTEGER NOT NULL,
  txt TEXT NOT NULL
);

SELECT *
FROM data
WHERE data.id = ANY (?);

SELECT *
FROM data
WHERE txt ILIKE ANY ('{"%bar", "%baz"}');

CREATE TABLE employees (
   id SERIAL PRIMARY KEY,
   first_name VARCHAR(255) NOT NULL,
   last_name VARCHAR(255) NOT NULL,
   salary DECIMAL(10, 2) NOT NULL
);

CREATE TABLE managers(
   id SERIAL PRIMARY KEY,
   first_name VARCHAR(255) NOT NULL,
   last_name VARCHAR(255) NOT NULL,
   salary DECIMAL(10, 2) NOT NULL
);

SELECT
  *
FROM
  employees
WHERE
  salary = ANY (
    SELECT
      salary
    FROM
      managers
  );

SELECT
  *
FROM
  employees
WHERE
  salary > ANY (
    SELECT
      salary
    FROM
      managers
  );
