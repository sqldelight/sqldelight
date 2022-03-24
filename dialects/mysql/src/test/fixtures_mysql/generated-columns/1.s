CREATE TABLE test(
  id VARCHAR(8)
);

ALTER TABLE test
  ADD COLUMN id_bigger_than_ten TINYINT(1) AS (id > 10) STORED NOT NULL,
  ADD COLUMN id_bigger_than_twenty TINYINT(1) AS (id > 20) VIRTUAL;
