CREATE TABLE scores (
  id INTEGER NOT NULL,
  name TEXT NOT NULL,
  points INTEGER NOT NULL
);

SELECT
  name,
  RANK() OVER (ORDER BY points DESC) rank_points,
  DENSE_RANK() OVER (ORDER BY points DESC) dense_rank_points,
  ROW_NUMBER() OVER (ORDER BY points DESC) row_num_points,
  LAG(points) OVER (ORDER BY points DESC) lag_points,
  LEAD(points) OVER (ORDER BY points DESC) lead_points,
  NTILE(6) OVER (ORDER BY points DESC) ntile_points,
  CUME_DIST() OVER (ORDER BY points DESC) cume_dist_points,
  PERCENT_RANK() OVER (ORDER BY points DESC) percent_rank_points
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
  ) AS running_total
FROM scores;

SELECT
  name,
  points,
  lag(points) OVER (
    PARTITION BY name
    ORDER BY points
    ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
  ) AS prev_point
FROM scores;

SELECT
  name,
  points,
  lag(points) OVER (
    PARTITION BY name
    ORDER BY points
    ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
  ) AS prev_point
FROM scores;
