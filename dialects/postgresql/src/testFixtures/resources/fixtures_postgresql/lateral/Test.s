CREATE TABLE A (
  b_id INTEGER
);

CREATE TABLE B (
  id INTEGER
);

SELECT * FROM A, LATERAL (SELECT * FROM B WHERE B.id = A.b_id) AB;

CREATE TABLE Author (
  id INTEGER PRIMARY KEY,
  name TEXT
);

CREATE TABLE Genre (
  id INTEGER PRIMARY KEY,
  name TEXT
);

CREATE TABLE Book (
  id INTEGER PRIMARY KEY,
  title TEXT,
  author_id INTEGER REFERENCES Author(id),
  genre_id INTEGER REFERENCES Genre(id)
);

SELECT
  Author.name AS author_name,
  Genre.name AS genre_name,
  book_count
FROM
  Author,
  Genre,
  LATERAL (
    SELECT
      COUNT(*) AS book_count
    FROM
      Book
    WHERE
      Book.author_id = Author.id
      AND Book.genre_id = Genre.id
  ) AS book_counts;

CREATE TABLE Kickstarter_Data (
    pledged INTEGER,
    fx_rate NUMERIC,
    backers_count INTEGER,
    launched_at NUMERIC,
    deadline NUMERIC,
    goal INTEGER
);

SELECT
    pledged_usd,
    avg_pledge_usd,
    duration,
    (usd_from_goal / duration) AS usd_needed_daily
FROM Kickstarter_Data,
    LATERAL (SELECT pledged / fx_rate AS pledged_usd) pu,
    LATERAL (SELECT pledged_usd / backers_count AS avg_pledge_usd) apu,
    LATERAL (SELECT goal / fx_rate AS goal_usd) gu,
    LATERAL (SELECT goal_usd - pledged_usd AS usd_from_goal) ufg,
    LATERAL (SELECT (deadline - launched_at) / 86400.00 AS duration) dr;

CREATE TABLE Regions (
  id INTEGER,
  name VARCHAR(255)
);

CREATE TABLE SalesPeople (
  id INTEGER,
  full_name VARCHAR(255),
  home_region_id INTEGER
);

CREATE TABLE Sales (
  id INTEGER,
  amount NUMERIC,
  product_id INTEGER,
  salesperson_id INTEGER,
  region_id INTEGER
);

SELECT
  sp.id salesperson_id,
  sp.full_name,
  sp.home_region_id,
  rg.name AS home_region_name,
  home_region_sales.total_sales
FROM SalesPeople sp
  JOIN Regions rg ON sp.home_region_id = rg.id
  JOIN LATERAL (
    SELECT SUM(amount) AS total_sales
    FROM Sales s
    WHERE s.salesperson_id = sp.id
      AND s.region_id = sp.home_region_id
  ) home_region_sales ON TRUE;
