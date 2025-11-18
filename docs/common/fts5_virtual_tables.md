# FTS5 Virtual Tables

You can define a FTS5 virtual table using the `CREATE VIRTUAL TABLE` statement.

```sql
CREATE VIRTUAL TABLE data USING fts5(
  text
);
```

When querying hidden columns within FTS5 virtual tables, you need to use a
workaround to map hidden columns correctly:

```sql
searchFTS5:
SELECT
  rank AS rank,
  rowid AS rowid
FROM data
WHERE data MATCH ?
ORDER BY rank;
```
