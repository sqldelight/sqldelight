CREATE TABLE T(
 a INT[],
 b INT[]
);

SELECT a @> ?, b <@ ?
FROM T;

SELECT *
FROM T
WHERE a @> ?;

SELECT *
FROM T
WHERE b <@ a;

SELECT *
FROM T
WHERE b && a;
