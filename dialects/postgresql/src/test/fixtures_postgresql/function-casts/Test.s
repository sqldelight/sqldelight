CREATE TABLE Posts(
  date_created TEXT NOT NULL
);

SELECT count(*) FROM Posts
WHERE age(CAST('epoch' AS TIMESTAMP) + date_created * CAST('1 millisecond' AS INTERVAL)) < CAST('1 week' AS INTERVAL);