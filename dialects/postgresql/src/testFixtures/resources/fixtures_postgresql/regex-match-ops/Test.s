CREATE TABLE regexops(
  t TEXT NOT NULL,
  c VARCHAR(50) NOT NULL,
  i INTEGER
);

SELECT concat(t, 'test') ~ ?, t ~* ?, t !~ ?, t !~* ?
FROM regexops;

SELECT t
FROM regexops
WHERE t ~ ?;

SELECT c
FROM regexops
WHERE c ~ ?;

--error[col 7]: operator ~ can only be performed on text
SELECT i ~ ?
FROM regexops;

