CREATE TABLE T (
    id INTEGER PRIMARY KEY
);

SELECT id, tableoid, xmin, cmin, xmax, cmax, ctid
FROM T;
