CREATE TABLE minMaxTable(
  first INTEGER,
  second INTEGER
);

SELECT MAX(first) FROM minMaxTable;
SELECT MIN(first) FROM minMaxTable;

-- error[col 7]: MAX only takes one argument
SELECT MAX(first, second) FROM minMaxTable;

-- error[col 7]: MIN only takes one argument
SELECT MIN(first, second) FROM minMaxTable;
