CREATE TABLE scores (
  id INTEGER NOT NULL,
  name TEXT NOT NULL,
  points INTEGER NOT NULL
);

SELECT
  name,
  RANK() OVER (ORDER BY points DESC) rank,
  DENSE_RANK() OVER (ORDER BY points DESC) dense_rank,
  ROW_NUMBER() OVER (ORDER BY points DESC) row_num,
  LAG(points) OVER (ORDER BY points DESC) lag,
  LEAD(points) OVER (ORDER BY points DESC) lead,
  NTILE(6) OVER (ORDER BY points DESC) ntile,
  CUME_DIST() OVER (ORDER BY points DESC) cume_dist,
  PERCENT_RANK() OVER (ORDER BY points DESC) percent_rank
FROM scores;

SELECT
  name,
  avg(points) OVER (
    PARTITION BY name
    ORDER BY points
    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
  ) AS moving_avg
FROM scores;

SELECT
  name,
  sum(points) OVER (
    PARTITION BY name
    ORDER BY points
    RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
  ) AS running_total
FROM scores;

SELECT
  name,
  sum(points) OVER (
    PARTITION BY name
    ORDER BY points
    RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    EXCLUDE CURRENT ROW
  ) AS running_total
FROM scores;

SELECT
  name,
  points,
  lag(points) OVER (
    PARTITION BY name
    ORDER BY points
    ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
    EXCLUDE GROUP
  ) AS prev_point
FROM scores;

SELECT
  name,
  points,
  lag(points) OVER (
    PARTITION BY name
    ORDER BY points
    ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
    EXCLUDE NO OTHERS
  ) AS prev_point
FROM scores;
