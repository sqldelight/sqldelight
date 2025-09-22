CREATE TABLE myTable(
    path LTREE NOT NULL,
    txt TEXT NOT NULL
);

SELECT path FROM myTable
WHERE path @> 'a.b.c';

SELECT path FROM myTable
WHERE path <@ 'a.b.c';

SELECT path FROM myTable
--error[col 6]:  expression must be ARRAY, JSONB, LTREE, TSVECTOR, TSRANGE, TSTZRANGE, TSMULTIRANGE, TSTZMULTIRANGE.
WHERE txt <@ 'a.b.c';

