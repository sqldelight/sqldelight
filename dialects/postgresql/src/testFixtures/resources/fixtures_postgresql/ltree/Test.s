CREATE TABLE myTable(
    path LTREE NOT NULL
);

SELECT path FROM myTable
WHERE path @> 'a.b.c';

SELECT path FROM myTable
WHERE path <@ 'a.b.c';

SELECT path FROM myTable
WHERE path @ 'a.*.c';

SELECT path FROM myTable
WHERE path ? 'a.*.c';
