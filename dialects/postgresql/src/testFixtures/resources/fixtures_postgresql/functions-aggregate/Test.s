CREATE TABLE myTable(
  myColumn REAL NOT NULL
);

SELECT percentile_disc(.5) WITHIN GROUP (ORDER BY myTable.myColumn) AS P5
FROM myTable;
