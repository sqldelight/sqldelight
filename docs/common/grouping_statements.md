You can group multiple SQL statements together to be execute at once:

```sql
upsert {
  UPDATE myTable
  SET column1 = :column1,
      column2 = :column2
  WHERE id = :id;

  INSERT OR IGNORE INTO myTable (id, column1, column2)
  VALUES (:column1, :column2, :column3);
}
```