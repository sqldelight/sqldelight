CREATE TABLE person (
  name TEXT NOT NULL
);

COPY person (name)
FROM STDIN
(
 FORMAT CSV,
 DELIMITER ';',
 HEADER TRUE
)
WHERE name = 'dev';
