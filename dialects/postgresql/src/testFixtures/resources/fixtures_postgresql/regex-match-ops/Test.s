CREATE TABLE regexops(
  t TEXT NOT NULL,
  i INTEGER
);

SELECT concat(t, 'test') ~ ?, t ~* ?, t !~ ?, t !~* ?
FROM regexops;

SELECT t
FROM regexops
WHERE t ~ ?;

--error[col 7]: operator does not exist: INTEGER ~ unknown
SELECT i ~ ?
FROM regexops;


